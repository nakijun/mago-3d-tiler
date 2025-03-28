package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.octree.GaiaOctreeVertices;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.model.GaiaVertex;
import com.gaia3d.basic.pointcloud.GaiaPointCloudHeader;
import com.gaia3d.basic.pointcloud.GaiaPointCloudTemp;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.util.GlobeUtils;
import com.github.mreutegg.laszip4j.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.locationtech.proj4j.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class LasConverter {
    public List<GaiaPointCloud> load(String path) {
        return convert(new File(path));
    }

    public List<GaiaPointCloud> load(File file) {
        return convert(file);
    }

    public List<GaiaPointCloud> load(Path path) {
        return convert(path.toFile());
    }

    public GaiaPointCloudHeader readHeader(File file) {
        LASReader reader = new LASReader(file);
        LASHeader header = reader.getHeader();

        double getMinX = header.getMinX();
        double getMinY = header.getMinY();
        double getMinZ = header.getMinZ();
        double getMaxX = header.getMaxX();
        double getMaxY = header.getMaxY();
        double getMaxZ = header.getMaxZ();
        Vector3d min = new Vector3d(getMinX, getMinY, getMinZ);
        Vector3d max = new Vector3d(getMaxX, getMaxY, getMaxZ);

        GaiaBoundingBox srsBoundingBox = new GaiaBoundingBox();
        srsBoundingBox.addPoint(min);
        srsBoundingBox.addPoint(max);

        long pointRecords = header.getNumberOfPointRecords();
        long legacyPointRecords = header.getLegacyNumberOfPointRecords();
        long totalPointRecords = pointRecords + legacyPointRecords;
        return GaiaPointCloudHeader.builder()
                .index(-1)
                .uuid(UUID.randomUUID())
                .size(totalPointRecords)
                .srsBoundingBox(srsBoundingBox)
                .build();
    }

    public void loadToTemp(GaiaPointCloudHeader pointCloudHeader, File file) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        LASReader reader = new LASReader(file);
        LASHeader header = reader.getHeader();
        double xScaleFactor = header.getXScaleFactor();
        double xOffset = header.getXOffset();
        double yScaleFactor = header.getYScaleFactor();
        double yOffset = header.getYOffset();
        double zScaleFactor = header.getZScaleFactor();
        double zOffset = header.getZOffset();

        CloseablePointIterable pointIterable = reader.getCloseablePoints();
        long pointRecords = header.getNumberOfPointRecords();
        long legacyPointRecords = header.getLegacyNumberOfPointRecords();
        long totalPointsSize = pointRecords + legacyPointRecords;

        byte recordFormatValue = header.getPointDataRecordFormat();
        boolean hasRgbColor;
        LasRecordFormat recordFormat = LasRecordFormat.fromFormatNumber(recordFormatValue);
        if (recordFormat != null) {
            hasRgbColor = recordFormat.hasColor;
        } else {
            hasRgbColor = false;
        }

        String version = header.getVersionMajor() + "." + header.getVersionMinor();
        String systemId = header.getSystemIdentifier();
        String softwareId = header.getGeneratingSoftware();
        String fileCreationDate = (short) header.getFileCreationYear() + "-" + (short) header.getFileCreationDayOfYear();
        String headerSize = (short) header.getHeaderSize() + " bytes";

        log.debug("Version: {}", version);
        log.debug("System ID: {}", systemId);
        log.debug("Software ID: {}", softwareId);
        log.debug("File Creation Date: {}", fileCreationDate);
        log.debug("Header Size: {}", headerSize);

        boolean isDefaultCrs = globalOptions.getCrs().equals(GlobalOptions.DEFAULT_CRS);
        header.getVariableLengthRecords().forEach((record) -> {
            if (isDefaultCrs && record.getUserID().equals("LASF_Projection")) {
                String wktCRS = record.getDataAsString();
                CoordinateReferenceSystem crs = GlobeUtils.convertWkt(wktCRS);
                if (crs != null) {
                    var convertedCrs = GlobeUtils.convertProj4jCrsFromGeotoolsCrs(crs);
                    globalOptions.setCrs(convertedCrs);
                    log.info(" - Coordinate Reference System : {}", wktCRS);
                } else {
                    String epsg = GlobeUtils.extractEpsgCodeFromWTK(wktCRS);
                    if (epsg != null) {
                        CRSFactory factory = new CRSFactory();
                        globalOptions.setCrs(factory.createFromName("EPSG:" + epsg));
                        log.info(" - Coordinate Reference System : {}", epsg);
                    }
                }
            }
        });

        int percentage = globalOptions.getPointRatio();
        if (percentage < 1) {
            percentage = 1;
        } else if (percentage > 100) {
            percentage = 100;
        }
        int volumeFactor = (int) Math.ceil(100 / percentage);
        int count = 0;
        for (LASPoint point : pointIterable) {
            if (count++ % volumeFactor != 0) {
                continue;
            }
            double x = point.getX() * xScaleFactor + xOffset;
            double y = point.getY() * yScaleFactor + yOffset;
            double z = point.getZ() * zScaleFactor + zOffset;
            byte[] rgb;

            if (hasRgbColor) {
                if (globalOptions.isForce4ByteRGB()) {
                    rgb = getColorByByteRGB(point); // only for test
                } else {
                    rgb = getColorByRGB(point);
                }
            } else {
                //rgb = getColorIntensity(point);
                rgb = getHeight(z);
            }
            Vector3d position = new Vector3d(x, y, z);

            GaiaVertex vertex = new GaiaVertex();
            vertex.setPosition(position);
            vertex.setColor(rgb);

            GaiaPointCloudTemp tempFile = pointCloudHeader.findTemp(position);
            if (tempFile == null) {
                log.error("[ERROR] Failed to find temp file.");
            } else {
                tempFile.writePosition(position, rgb);
            }
        }
    }

    // Detail Volume
    private int calcOctreeVolume(int sampleSize, CloseablePointIterable pointIterable, Vector3d scale, Vector3d offset) {
        List<GaiaVertex> vertices = new ArrayList<>();
        int count = 0;
        for (LASPoint point : pointIterable) {
            if (count++ % sampleSize != 0) {
                continue;
            } else {
                double x = point.getX();
                double y = point.getY();
                double z = point.getZ();

                x = x * scale.x + offset.x;
                y = y * scale.y + offset.y;
                z = z * scale.z + offset.z;
                Vector3d position = new Vector3d(x, y, z);
                GaiaVertex vertex = new GaiaVertex();
                vertex.setPosition(position);
                vertices.add(vertex);
            }
        }

        double length = 1.0;
        GaiaOctreeVertices octreeVertices = new GaiaOctreeVertices(null);
        octreeVertices.setVertices(vertices);
        octreeVertices.calculateSize();
        octreeVertices.setMaxDepth(25);

        /*octreeVertices.setMaxX(length);
        octreeVertices.setMaxY(length);
        octreeVertices.setMaxZ(length);*/
        octreeVertices.makeTreeByMinBoxSize(length); // 1m

        List<GaiaOctreeVertices> result = new ArrayList<>();
        octreeVertices.extractOctreesWithContents(result);

        return result.size();
    }

    private List<GaiaPointCloud> convert(File file) {
        List<GaiaPointCloud> pointClouds = new ArrayList<>();
        GaiaPointCloud pointCloud = new GaiaPointCloud();
        GaiaBoundingBox boundingBox = pointCloud.getGaiaBoundingBox();
        try {
            GaiaPointCloudTemp readTemp = new GaiaPointCloudTemp(file);
            readTemp.readHeader();

            double[] quantizationOffset = readTemp.getQuantizedVolumeOffset();
            double[] quantizationScale = readTemp.getQuantizedVolumeScale();
            double[] originalMinPosition = new double[]{quantizationOffset[0], quantizationOffset[1], quantizationOffset[2]};
            double[] originalMaxPosition = new double[]{quantizationOffset[0] + quantizationScale[0], quantizationOffset[1] + quantizationScale[1], quantizationOffset[2] + quantizationScale[2]};
            Vector3d minPosition = new Vector3d(originalMinPosition[0], originalMinPosition[1], originalMinPosition[2]);
            Vector3d maxPosition = new Vector3d(originalMaxPosition[0], originalMaxPosition[1], originalMaxPosition[2]);

            boundingBox.addPoint(minPosition);
            boundingBox.addPoint(maxPosition);

            readTemp.getInputStream().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        GaiaPointCloudTemp readTemp = new GaiaPointCloudTemp(file);
        pointCloud.setMinimized(true);
        pointCloud.setVertices(null);
        pointCloud.setGaiaBoundingBox(boundingBox);
        pointCloud.setPointCloudTemp(readTemp);
        pointClouds.add(pointCloud);
        return pointClouds;
    }

    /**
     * Reduce points by using HashMap
     * @param vertices List<GaiaVertex>
     * @return List<GaiaVertex>
     */
    private List<GaiaVertex> reducePointsA(List<GaiaVertex> vertices) {
        String format = "%.5f";
        Map<String, GaiaVertex> hashMap = new HashMap<>();
        vertices.forEach((vertex) -> {
            Vector3d position = vertex.getPosition();
            double x = position.x;
            double y = position.y;
            double z = position.z;

            String xStr = String.format(format, x);
            String yStr = String.format(format, y);
            String zStr = String.format(format, z);
            String key = xStr + "-" + yStr + "-" + zStr;
            if (hashMap.containsKey(key)) {

            } else {
                hashMap.put(key, vertex);
            }
        });
        List<GaiaVertex> newVertices = new ArrayList<>(hashMap.values());
        return newVertices;
    }

    /**
     * Get color by RGB
     * @param point LASPoint
     * @return byte[3]
     */
    private byte[] getColorByRGB(LASPoint point) {
        double red = (double) point.getRed() / 65535;
        double green = (double) point.getGreen() / 65535;
        double blue = (double) point.getBlue() / 65535;

        byte[] rgb = new byte[3];
        rgb[0] = (byte) (red * 255);
        rgb[1] = (byte) (green * 255);
        rgb[2] = (byte) (blue * 255);
        return rgb;
    }

    /**
     * Get color by RGB
     * @param point LASPoint
     * @return byte[3]
     */
    private byte[] getColorByByteRGB(LASPoint point) {
        byte[] rgb = new byte[3];
        rgb[0] = (byte) point.getRed();
        rgb[1] = (byte) point.getGreen();
        rgb[2] = (byte) point.getBlue();

        return rgb;
    }

    /**
     * Get color by intensity (Gray scale)
     * @param point LASPoint
     * @return byte[3]
     */
    private byte[] getColorIntensity(LASPoint point) {
        char intensity = point.getIntensity();
        double intensityDouble = (double) intensity / 65535;

        byte color = (byte) (intensityDouble * 255);
        byte[] rgb = new byte[3];
        rgb[0] = color;
        rgb[1] = color;
        rgb[2] = color;
        return rgb;
    }

    /**
     *
     * @param point LASPoint
     * @return byte[3]
     */
    private byte[] getClassification(LASPoint point) {
        short classification = point.getClassification();
        double classificatgionDouble = (double) classification / 65535;

        byte color = (byte) (classificatgionDouble * 255);
        byte[] rgb = new byte[3];
        rgb[0] = color;
        rgb[1] = color;
        rgb[2] = color;
        return rgb;
    }

    private byte[] getHeight(double z) {
        byte color = (byte) (z / 65535 * 255);
        byte[] rgb = new byte[3];
        rgb[0] = color;
        rgb[1] = color;
        rgb[2] = color;
        return rgb;
    }

}
