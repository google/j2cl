/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.benchmarks.octane.raytrace;

import com.google.j2cl.benchmarks.octane.raytrace.Engine.Options;

/** Ray tracer benchmark based on code by Adam Burmister. */
public final class RayTrace {
  // Variable used to hold a number that can be used to verify that the scene was ray traced
  // correctly.
  public static int checkNumber = 0;

  public static void renderScene() {
    Scene scene = new Scene();
    scene.setCamera(new Camera(new Vector(0, 0, -15), new Vector(-0.2, 0, 5), new Vector(0, 1, 0)));
    scene.setBackground(new Background(new Color(0.5, 0.5, 0.5), 0.4));

    Sphere sphere =
        new Sphere(
            new Vector(-1.5, 1.5, 2),
            1.5,
            new MaterialSolid(new Color(0, 0.5, 0.5), 0.3, 0.0, 2.0));
    Sphere sphere1 =
        new Sphere(
            new Vector(1, 0.25, 1),
            0.5,
            new MaterialSolid(new Color(0.9, 0.9, 0.9), 0.1, 0.0, 1.5));
    Plane plane =
        new Plane(
            new Vector(0.1, 0.9, -0.5).normalize(),
            1.2,
            new Chessboard(new Color(1, 1, 1), new Color(0, 0, 0), 0.2, 0.0, 1.0, 0.7));
    scene.setShapes(plane, sphere, sphere1);

    Light light = new Light(new Vector(5, 10, -1), new Color(0.8, 0.8, 0.8));
    Light light1 = new Light(new Vector(-3, 5, -15), new Color(0.8, 0.8, 0.8));
    scene.setLights(light, light1);

    Options options = new Engine.Options();
    options.canvasWidth = 100;
    options.canvasHeight = 100;
    options.pixelWidth = 5;
    options.pixelHeight = 5;
    options.renderDiffuse = true;
    options.renderShadows = true;
    options.renderHighlights = true;
    options.renderReflections = true;
    options.rayDepth = 2;

    checkNumber = 0;

    new Engine(options).renderScene(scene);

    if (checkNumber != 2321) {
      throw new AssertionError("Scene rendered incorrectly");
    }
  }

  private RayTrace() {}
}
