/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
 package com.nav.arannotationpoc.common.rendering;

import android.opengl.Matrix;

import com.nav.arannotationpoc.common.helpers.AppSettings;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

public class LineUtils {

    /**
     * @param value
     * @param inputMin
     * @param inputMax
     * @param outputMin
     * @param outputMax
     * @param clamp
     * @return
     */
    public static float map(float value, float inputMin, float inputMax, float outputMin, float outputMax, boolean clamp) {
            float outVal = ((value - inputMin) / (inputMax - inputMin) * (outputMax - outputMin) + outputMin);

            if (clamp) {
                if (outputMax < outputMin) {
                    if (outVal < outputMax) outVal = outputMax;
                    else if (outVal > outputMin) outVal = outputMin;
                } else {
                    if (outVal > outputMax) outVal = outputMax;
                    else if (outVal < outputMin) outVal = outputMin;
                }
            }
            return outVal;
    }

    /**
     * @param start
     * @param stop
     * @param amt
     * @return
     */
    public static float lerp(float start, float stop, float amt) {
        return start + (stop - start) * amt;
    }

    /**
     * @param touchPoint
     * @param screenWidth
     * @param screenHeight
     * @param projectionMatrix
     * @param viewMatrix
     * @return
     */
    public static Vector3f GetWorldCoords(Vector2f touchPoint, float screenWidth, float screenHeight, float[] projectionMatrix, float[] viewMatrix) {
        Ray touchRay = projectRay(touchPoint, screenWidth, screenHeight, projectionMatrix, viewMatrix);
        touchRay.direction.scale(AppSettings.getStrokeDrawDistance());
        touchRay.origin.add(touchRay.direction);
        return touchRay.origin;
    }

    // Assume 'projectionMatrix' is a float array containing the 4x4 matrix values

    private static float getFarClipDistance(float[] projectionMatrix) {
        // Assuming the projectionMatrix is a 16-element 1D float array (4x4 matrix flattened)

        // Perspective projection matrix typically has the far clip distance in this position
        // In a typical matrix format:
        // projectionMatrix[10] is usually the near plane depth (or other projection values)
        // projectionMatrix[14] contains the far plane distance
        return projectionMatrix[14];
    }

    public static List<Vector3f> GetWorldCoords2(
            Vector2f touchPoint, float screenWidth, float screenHeight,
            float[] projectionMatrix, float[] viewMatrix,
            Vector3f planePosition, Vector3f planeNormal) {

        // Project the ray from the touch point
        Ray touchRay = projectRay(touchPoint, screenWidth, screenHeight, projectionMatrix, viewMatrix);

        if (touchRay.origin == null || planePosition == null) {
            System.err.println("GetWorldCoords: Null reference encountered - touchRay or planePosition is null.");
            return null;
        }

        Vector3f rayOrigin = touchRay.origin;
        Vector3f rayDirection = touchRay.direction;

        // Calculate the intersection of the ray with the specified plane
        float denom = planeNormal.dot(rayDirection);

        if (Math.abs(denom) > 1e-6) { // Check if ray is not parallel to the plane
            Vector3f originToPlane = new Vector3f();
            originToPlane.sub(planePosition, rayOrigin); // Vector from ray origin to plane position
            float t = originToPlane.dot(planeNormal) / denom;

            if (t >= 0) { // Intersection is in the positive direction of the ray
                List<Vector3f> linePoints = new ArrayList<>();
                float step = t / 10; // Dividing ray into segments

                for (float i = 0; i <= t; i += step) {
                    Vector3f pointOnRay = new Vector3f(rayDirection);
                    pointOnRay.scale(i);
                    pointOnRay.add(rayOrigin);
                    linePoints.add(pointOnRay);
                }

                // Optionally add the exact intersection point if needed:
                Vector3f intersectionPoint = new Vector3f(rayDirection);
                intersectionPoint.scale(t);
                intersectionPoint.add(rayOrigin);
                linePoints.add(intersectionPoint);

                return linePoints;
            }
        }

        return null;
    }


    /**
     * @param point
     * @param viewportSize
     * @param viewProjMtx
     * @return
     */
    public static Ray screenPointToRay(Vector2f point, Vector2f viewportSize, float[] viewProjMtx) {
        point.y = viewportSize.y - point.y;
        float x = point.x * 2.0F / viewportSize.x - 1.0F;
        float y = point.y * 2.0F / viewportSize.y - 1.0F;
        float[] farScreenPoint = new float[]{x, y, 1.0F, 1.0F};
        float[] nearScreenPoint = new float[]{x, y, -1.0F, 1.0F};
        float[] nearPlanePoint = new float[4];
        float[] farPlanePoint = new float[4];
        float[] invertedProjectionMatrix = new float[16];
        Matrix.setIdentityM(invertedProjectionMatrix, 0);
        Matrix.invertM(invertedProjectionMatrix, 0, viewProjMtx, 0);
        Matrix.multiplyMV(nearPlanePoint, 0, invertedProjectionMatrix, 0, nearScreenPoint, 0);
        Matrix.multiplyMV(farPlanePoint, 0, invertedProjectionMatrix, 0, farScreenPoint, 0);
        Vector3f direction = new Vector3f(farPlanePoint[0] / farPlanePoint[3], farPlanePoint[1] / farPlanePoint[3], farPlanePoint[2] / farPlanePoint[3]);
        Vector3f origin = new Vector3f(new Vector3f(nearPlanePoint[0] / nearPlanePoint[3], nearPlanePoint[1] / nearPlanePoint[3], nearPlanePoint[2] / nearPlanePoint[3]));
        direction.sub(origin);
        direction.normalize();
        return new Ray(origin, direction);
    }

    /**
     * @param touchPoint
     * @param screenWidth
     * @param screenHeight
     * @param projectionMatrix
     * @param viewMatrix
     * @return
     */
    public static Ray projectRay(Vector2f touchPoint, float screenWidth, float screenHeight, float[] projectionMatrix, float[] viewMatrix) {
        float[] viewProjMtx = new float[16];
        Matrix.multiplyMM(viewProjMtx, 0, projectionMatrix, 0, viewMatrix, 0);
        return screenPointToRay(touchPoint, new Vector2f(screenWidth, screenHeight), viewProjMtx);
    }


    /**
     * @param newPoint
     * @param lastPoint
     * @return
     */
    public static boolean distanceCheck(Vector3f newPoint, Vector3f lastPoint) {
        Vector3f temp = new Vector3f();
        temp.sub(newPoint, lastPoint);
        if (temp.length() > AppSettings.getMinDistance()) {
            return true;
        }
        return false;
    }
}
