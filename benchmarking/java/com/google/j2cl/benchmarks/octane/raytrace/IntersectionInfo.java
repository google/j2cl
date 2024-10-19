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

class IntersectionInfo {

  private boolean isHit;
  private Shape shape;
  private Vector position;
  private Vector normal;
  private Color color;
  private double distance;

  IntersectionInfo() {
    this.color = new Color(0, 0, 0);
  }

  void isHit(boolean b) {
    this.isHit = b;
  }

  boolean isHit() {
    return isHit;
  }

  void setDistance(double distance) {
    this.distance = distance;
  }

  void setShape(Shape shape) {
    this.shape = shape;
  }

  double getDistance() {
    return distance;
  }

  void setPosition(Vector position) {
    this.position = position;
  }

  Vector getPosition() {
    return position;
  }

  void setNormal(Vector normal) {
    this.normal = normal;
  }

  void setColor(Color color) {
    this.color = color;
  }

  @JsNonNull
  @Override
  public String toString() {
    return "Intersection [" + this.position + "]";
  }

  Color getColor() {
    return color;
  }

  Shape getShape() {
    return shape;
  }

  Vector getNormal() {
    return normal;
  }
}
