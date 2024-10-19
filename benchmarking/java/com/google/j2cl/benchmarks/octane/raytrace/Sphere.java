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

class Sphere extends Shape {

  private final double radius;
  private final Vector position;
  private final BaseMaterial material;

  Sphere(Vector pos, double radius, BaseMaterial material) {
    this.radius = radius;
    this.position = pos;
    this.material = material;
  }

  @Override
  IntersectionInfo intersect(Ray ray) {
    IntersectionInfo info = new IntersectionInfo();
    info.setShape(this);

    Vector dst = Vector.subtract(ray.getPosition(), position);

    double b = dst.dot(ray.getDirection());
    double c = dst.dot(dst) - (radius * radius);
    double d = (b * b) - c;

    if (d > 0) { // intersection!
      info.isHit(true);
      info.setDistance(-b - Math.sqrt(d));
      info.setPosition(
          Vector.add(
              ray.getPosition(), Vector.multiplyScalar(ray.getDirection(), info.getDistance())));

      info.setNormal(Vector.subtract(info.getPosition(), position).normalize());

      info.setColor(material.getColor(0, 0));
    } else {
      info.isHit(false);
    }
    return info;
  }

  @Override
  BaseMaterial getMaterial() {
    return material;
  }

  @Override
  Vector getPosition() {
    return this.position;
  }
}
