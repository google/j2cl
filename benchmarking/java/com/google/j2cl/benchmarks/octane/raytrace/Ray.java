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

class Ray {

  private final Vector position;
  private final Vector direction;

  Ray(Vector pos, Vector dir) {
    this.position = pos;
    this.direction = dir;
  }

  @JsNonNull
  @Override
  public String toString() {
    return "Ray [" + position + "," + direction + "]";
  }

  Vector getPosition() {
    return position;
  }

  Vector getDirection() {
    return direction;
  }
}
