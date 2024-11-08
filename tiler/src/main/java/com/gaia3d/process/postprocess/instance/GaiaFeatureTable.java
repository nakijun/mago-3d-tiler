package com.gaia3d.process.postprocess.instance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gaia3d.process.postprocess.pointcloud.BatchId;
import com.gaia3d.process.postprocess.pointcloud.Color;
import com.gaia3d.process.postprocess.pointcloud.Position;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GaiaFeatureTable {
    @JsonProperty("BATCH_LENGTH")
    private int batchLength;



    @JsonProperty("RTC_CENTER")
    private double[] rctCenter = new double[3];

    /* Instanced3DModel */
    @JsonProperty("INSTANCES_LENGTH")
    private int instancesLength;
    @JsonProperty("EAST_NORTH_UP")
    private boolean eastNorthUp;
    @JsonProperty("NORMAL_RIGHT")
    private Normal normalRight;
    @JsonProperty("NORMAL_UP")
    private Normal normalUp;
    @JsonProperty("SCALE")
    private Scale scale;

    /* PointCloud */
    // POSITION = POSITION_QUANTIZED * QUANTIZED_VOLUME_SCALE / 65535.0 + QUANTIZED_VOLUME_OFFSET
    @JsonProperty("POINTS_LENGTH")
    private int pointsLength;
    @JsonProperty("POSITION_QUANTIZED")
    private Position positionQuantized;
    @JsonProperty("QUANTIZED_VOLUME_SCALE")
    private float[] quantizedVolumeScale = new float[3];
    @JsonProperty("QUANTIZED_VOLUME_OFFSET")
    private float[] quantizedVolumeOffset = new float[3];

    @JsonProperty("POSITION")
    private Position position;
    @JsonProperty("RGB")
    private Color color;
    @JsonProperty("BATCH_ID")
    private BatchId batchId;
}
