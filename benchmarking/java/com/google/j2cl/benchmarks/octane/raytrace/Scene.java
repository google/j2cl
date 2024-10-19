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

class Scene {

  private Camera camera;
  private Shape[] shapes;
  private Light[] lights;
  private Background background;

  Scene() {
    this.camera = new Camera(new Vector(0, 0, -5), new Vector(0, 0, 1), new Vector(0, 1, 0));
    this.shapes = new Shape[0];
    this.lights = new Light[0];
    this.background = new Background(new Color(0, 0, 0.5), 0.2);
  }

  Camera getCamera() {
    return camera;
  }

  Background getBackground() {
    return background;
  }

  Shape[] getShapes() {
    return shapes;
  }

  Shape getShape(int i) {
    return shapes[i];
  }

  Light[] getLights() {
    return lights;
  }

  Light getLight(int i) {
    return lights[i];
  }

  void setCamera(Camera camera) {
    this.camera = camera;
  }

  void setBackground(Background background) {
    this.background = background;
  }

  void setShapes(Shape... shapes) {
    this.shapes = shapes;
  }

  void setLights(Light... lights) {
    this.lights = lights;
  }
}
