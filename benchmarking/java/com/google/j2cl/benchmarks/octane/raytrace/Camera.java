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

import jsinterop.annotations.JsNonNull;

class Camera {
  private final Vector position;
  private final Vector up;
  private final Vector equator;
  private final Vector screen;

  Camera(Vector pos, Vector lookAt, Vector up) {
    position = pos;
    this.up = up;
    equator = lookAt.normalize().cross(up);
    screen = Vector.add(position, lookAt);
  }

  Ray getRay(double vx, double vy) {
    Vector pos =
        Vector.subtract(
            screen,
            Vector.subtract(Vector.multiplyScalar(equator, vx), Vector.multiplyScalar(up, vy)));
    pos = new Vector(pos.getX(), -pos.getY(), pos.getZ());
    Vector dir = Vector.subtract(pos, position);

    return new Ray(pos, dir.normalize());
  }

  @JsNonNull
  @Override
  public String toString() {
    return "Ray []";
  }

  Vector getPosition() {
    return position;
  }
}
