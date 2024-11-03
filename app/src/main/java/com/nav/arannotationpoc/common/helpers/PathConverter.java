package com.nav.arannotationpoc.common.helpers;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.opengl.Matrix;

import com.google.ar.core.Anchor;

import java.util.ArrayList;
import java.util.List;

public class PathConverter {

    public static List<float[]> convertPathTo3D(Path path, Anchor anchor) {
        List<float[]> points = new ArrayList<>();

        // Use PathMeasure to get the points along the path
        PathMeasure pathMeasure = new PathMeasure(path, false);
        float pathLength = pathMeasure.getLength();
        float[] point = new float[2];

        // Define step size to sample points along the path
        float step = 10f;  // Adjust for density
        float[] anchorMatrix = new float[16];
        anchor.getPose().toMatrix(anchorMatrix, 0);

        for (float distance = 0; distance < pathLength; distance += step) {
            if (pathMeasure.getPosTan(distance, point, null)) {
                // Transform screen coordinates into world coordinates
                float[] worldPoint = new float[4];
                Matrix.multiplyMV(worldPoint, 0, anchorMatrix, 0, new float[]{point[0], point[1], 0, 1f}, 0);
                points.add(new float[]{worldPoint[0], worldPoint[1], worldPoint[2]});
            }
        }

        return points;
    }
}
