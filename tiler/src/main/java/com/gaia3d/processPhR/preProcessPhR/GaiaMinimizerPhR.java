package com.gaia3d.processPhR.preProcessPhR;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.halfedge.HalfEdgeUtils;
import com.gaia3d.basic.halfedge.PlaneType;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
public class GaiaMinimizerPhR implements PreProcess {
    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        if (scene != null) {
            log.info("Welding vertices in GaiaScene");

            // 1rst, must weld vertices.***
            boolean checkTexCoord = true;
            boolean checkNormal = false;
            boolean checkColor = false;
            boolean checkBatchId = false;
            double error = 1e-8;
            scene.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);

//            // Test create a rectangularNet scene.***
//            boolean calculateTexCoords = true;
//            int numCols = 157;
//            int numRows = 214;
//            double width = 100;
//            double height = 100;
//            GaiaScene rectScene = GaiaSceneUtils.getSceneRectangularNet(numCols, numRows, width, height, calculateTexCoords);
//            GaiaNode rootNode = rectScene.getNodes().get(0);
//            GaiaNode node = rootNode.getChildren().get(0);
//            GaiaMesh mesh = node.getMeshes().get(0);
//            GaiaPrimitive primitive = mesh.getPrimitives().get(0);
//            Vector3d translate = new Vector3d(0, 0, 20);
//            primitive.translate(translate);
//            primitive.setMaterialIndex(0);
//            scene.getNodes().clear();
//            scene.getNodes().add(rootNode);
//            // End test.------------------------

            // Test.************************************************
//            GaiaNode rootNode = scene.getNodes().get(0);
//            List<GaiaNode> reducedChildren = new ArrayList<>();
//            int childrenCount = rootNode.getChildren().size();
//
//            reducedChildren.add(rootNode.getChildren().get(100));
//            rootNode.getChildren().clear();
//            rootNode.setChildren(reducedChildren);
            // End test.--------------------------------------------

            List<Path> tempPathLod = new ArrayList<>();
            Path tempFolder = tileInfo.getTempPath();

            // Lod 0.************************************************************************************************************
            log.info("Minimize GaiaScene LOD 0");

//                        // test.***
//                        HalfEdgeScene halfEdgeSceneLod0 = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);
//                        halfEdgeSceneLod0.TEST_cutScene();
//                        GaiaScene sceneLod0 = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeSceneLod0);
//                        // end test.***

            GaiaSet tempSetLod0 = GaiaSet.fromGaiaScene(scene);
            Path tempPathLod0 = tempSetLod0.writeFile(tileInfo.getTempPath(), tileInfo.getSerial(), tempSetLod0.getAttribute());
            tileInfo.setTempPath(tempPathLod0);
            tempPathLod.add(tempPathLod0);

            // Lod 1.************************************************************************************************************
            log.info("Minimize GaiaScene LOD 1");
            log.info("Making HalfEdgeScene from GaiaScene");
            HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);

            log.info("Doing triangles reduction in HalfEdgeScene");
            halfEdgeScene.doTrianglesReduction();

            //halfEdgeScene.TEST_cutScene();


            log.info("Making GaiaScene from HalfEdgeScene");
            GaiaScene sceneLod1 = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
            halfEdgeScene.deleteObjects();

            GaiaSet tempSetLod1 = GaiaSet.fromGaiaScene(sceneLod1);

            Path tempFolderLod1 = tempFolder.resolve("lod1");
            Path tempPathLod1 = tempSetLod1.writeFile(tempFolderLod1, tileInfo.getSerial(), tempSetLod1.getAttribute());
            tempPathLod.add(tempPathLod1);


            // Lod 2.************************************************************************************************************
            log.info("Minimize GaiaScene LOD 2");
            checkTexCoord = false;
            scene.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);

            log.info("Making HalfEdgeScene from GaiaScene");
            halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);

            log.info("Doing triangles reduction in HalfEdgeScene");
            halfEdgeScene.doTrianglesReduction();

            log.info("Making GaiaScene from HalfEdgeScene");
            GaiaScene sceneLod2 = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
            halfEdgeScene.deleteObjects();

            GaiaSet tempSetLod2 = GaiaSet.fromGaiaScene(sceneLod2);

            Path tempFolderLod2 = tempFolder.resolve("lod2");
            Path tempPathLod2 = tempSetLod2.writeFile(tempFolderLod2, tileInfo.getSerial(), tempSetLod2.getAttribute());
            tempPathLod.add(tempPathLod2);

            // set tempPathLod to tileInfo.***
            tileInfo.setTempPathLod(tempPathLod);

            if (tempSetLod0 != null) {
                tempSetLod0.clear();
                tempSetLod0 = null;
            }

            if (scene != null) {
                scene.clear();
                scene = null;
            }

        }
        return tileInfo;
    }

    private List<HalfEdgeScene> testCutHalfEdgeScene(HalfEdgeScene halfEdgeScene) {
        List<HalfEdgeScene> halfEdgeCutScenes = null;
        PlaneType planeType = PlaneType.XZ;

//        if(halfEdgeScene.cutByPlane(planeType, samplePointLC, error))
//        {
//            deletedTileInfoMap.put(tileInfo, tileInfo);
//            // once scene is cut, then save the 2 scenes and delete the original.***
//            halfEdgeScene.classifyFacesIdByPlane(planeType, samplePointLC);
//
//            halfEdgeCutScenes = HalfEdgeUtils.getCopyHalfEdgeScenesByFaceClassifyId(halfEdgeScene, null);
//
//            // create tileInfos for the cut scenes.***
//            for(HalfEdgeScene halfEdgeCutScene : halfEdgeCutScenes)
//            {
//                GaiaScene gaiaSceneCut = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeCutScene);
//
//                // create an originalPath for the cut scene.***
//                Path cutScenePath = Paths.get("");
//                gaiaSceneCut.setOriginalPath(cutScenePath);
//
//                GaiaSet gaiaSetCut = GaiaSet.fromGaiaScene(gaiaSceneCut);
//                UUID identifier = UUID.randomUUID();
//                Path gaiaSetCutFolderPath = cutTempLodPath.resolve(identifier.toString());
//                if(!gaiaSetCutFolderPath.toFile().exists())
//                {
//                    gaiaSetCutFolderPath.toFile().mkdirs();
//                }
//
//                Path tempPathLod = gaiaSetCut.writeFile(gaiaSetCutFolderPath);
//
//                // create a new tileInfo for the cut scene.***
//                TileInfo tileInfoCut = TileInfo.builder().scene(gaiaSceneCut).outputPath(tileInfo.getOutputPath()).build();
//                tileInfoCut.setTempPath(tempPathLod);
//
//                // make a kmlInfo for the cut scene.***
//                // In reality, we must recalculate the position of the cut scene. Provisionally, we use the same position.***
//                // In reality, we must recalculate the position of the cut scene. Provisionally, we use the same position.***
//                KmlInfo kmlInfoCut = KmlInfo.builder().position(geoCoordPosition).build();
//                tileInfoCut.setKmlInfo(kmlInfoCut);
//                cutTileInfos.add(tileInfoCut);
//            }
//
//        }

        return halfEdgeCutScenes;
    }
}
