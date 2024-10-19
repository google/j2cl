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

abstract class BaseMaterial {
  double gloss = 2.0;
  double transparency = 0.0; // 0=opaque
  double reflection = 0.0; // [0...infinity] 0 = no reflection
  boolean hasTexture = false;

  abstract Color getColor(double u, double v);

  double wrapUp(double t) {
    t = t % 2.0;
    if (t < -1) {
      t += 2.0;
    } else if (t >= 1) {
      t -= 2.0;
    }
    return t;
  }

  @JsNonNull
  @Override
  public String toString() {
    return "Material [gloss="
        + gloss
        + ", transparency="
        + transparency
        + ", hasTexture="
        + hasTexture
        + "]";
  }
}
