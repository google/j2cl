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

class Vector {
  private final double x;
  private final double y;
  private final double z;

  Vector(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  Vector normalize() {
    double m = magnitude();
    return new Vector(x / m, y / m, z / m);
  }

  double magnitude() {
    return Math.sqrt((x * x) + (y * y) + (z * z));
  }

  Vector cross(Vector w) {
    return new Vector(-z * w.y + y * w.z, z * w.x - x * w.z, -y * w.x + x * w.y);
  }

  double dot(Vector w) {
    return x * w.x + y * w.y + z * w.z;
  }

  static Vector add(Vector v, Vector w) {
    return new Vector(w.x + v.x, w.y + v.y, w.z + v.z);
  }

  static Vector subtract(Vector v, Vector w) {
    return new Vector(v.x - w.x, v.y - w.y, v.z - w.z);
  }

  static Vector multiplyScalar(Vector v, double w) {
    return new Vector(v.x * w, v.y * w, v.z * w);
  }

  @JsNonNull
  @Override
  public String toString() {
    return "Vector [" + x + "," + y + "," + z + "]";
  }

  // getters are added for a real Java style benchmark
  double getX() {
    return x;
  }

  double getY() {
    return y;
  }

  double getZ() {
    return z;
  }
}
