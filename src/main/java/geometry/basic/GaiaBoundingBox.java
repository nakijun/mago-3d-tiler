package geometry.basic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3f;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaBoundingBox {
    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;

    //getCenterVector3d
    public Vector3d getCenter() {
        return new Vector3d((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }

    //setInit
    public void setInit(Vector3d vector3d) {
        minX = vector3d.x;
        minY = vector3d.y;
        minZ = vector3d.z;
        maxX = vector3d.x;
        maxY = vector3d.y;
        maxZ = vector3d.z;
    }

    //addPoint
    public void addPoint(Vector3d vector3d) {
        if (vector3d.x < minX) {
            minX = vector3d.x;
        }
        if (vector3d.y < minY) {
            minY = vector3d.y;
        }
        if (vector3d.z < minZ) {
            minZ = vector3d.z;
        }
        if (vector3d.x > maxX) {
            maxX = vector3d.x;
        }
        if (vector3d.y > maxY) {
            maxY = vector3d.y;
        }
        if (vector3d.z > maxZ) {
            maxZ = vector3d.z;
        }
    }

    //addBoundingBox
    public void addBoundingBox(GaiaBoundingBox boundingBox) {
        if (boundingBox.minX < minX) {
            minX = boundingBox.minX;
        }
        if (boundingBox.minY < minY) {
            minY = boundingBox.minY;
        }
        if (boundingBox.minZ < minZ) {
            minZ = boundingBox.minZ;
        }
        if (boundingBox.maxX > maxX) {
            maxX = boundingBox.maxX;
        }
        if (boundingBox.maxY > maxY) {
            maxY = boundingBox.maxY;
        }
        if (boundingBox.maxZ > maxZ) {
            maxZ = boundingBox.maxZ;
        }
    }
}
