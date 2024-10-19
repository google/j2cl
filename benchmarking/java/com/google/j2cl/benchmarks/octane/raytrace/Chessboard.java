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

class Chessboard extends BaseMaterial {

  private final double density;
  private final Color colorOdd;
  private final Color colorEven;

  Chessboard(
      Color colorEven,
      Color colorOdd,
      double reflection,
      double transparency,
      double gloss,
      double density) {
    this.colorEven = colorEven;
    this.colorOdd = colorOdd;
    this.reflection = reflection;
    this.transparency = transparency;
    this.gloss = gloss;
    this.density = density;
    this.hasTexture = true;
  }

  @Override
  Color getColor(double u, double v) {
    double t = wrapUp(u * density) * wrapUp(v * density);

    if (t < 0.0) {
      return colorEven;
    }
    return colorOdd;
  }

  @JsNonNull
  @Override
  public String toString() {
    return "ChessMaterial [gloss="
        + gloss
        + ", transparency="
        + transparency
        + ", hasTexture="
        + hasTexture
        + "]";
  }
}
