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

import static java.lang.Math.min;

import jsinterop.annotations.JsNonNull;

class Color {

  private double red;
  private double green;
  private double blue;

  Color(double r, double g, double b) {
    red = r;
    green = g;
    blue = b;
  }

  static Color add(Color c1, Color c2) {
    Color result = new Color(0, 0, 0);

    result.red = c1.red + c2.red;
    result.green = c1.green + c2.green;
    result.blue = c1.blue + c2.blue;

    return result;
  }

  static Color addScalar(Color c1, double s) {
    Color result = new Color(0, 0, 0);

    result.red = c1.red + s;
    result.green = c1.green + s;
    result.blue = c1.blue + s;

    result.limit();

    return result;
  }

  static Color multiply(Color c1, Color c2) {
    Color result = new Color(0, 0, 0);

    result.red = c1.red * c2.red;
    result.green = c1.green * c2.green;
    result.blue = c1.blue * c2.blue;

    return result;
  }

  static Color multiplyScalar(Color c1, double f) {
    Color result = new Color(0, 0, 0);

    result.red = c1.red * f;
    result.green = c1.green * f;
    result.blue = c1.blue * f;

    return result;
  }

  void limit() {
    red = (red > 0.0) ? min(red, 1.0) : 0.0;
    green = (green > 0.0) ? min(green, 1.0) : 0.0;
    blue = (blue > 0.0) ? min(blue, 1.0) : 0.0;
  }

  static Color blend(Color c1, Color c2, double w) {
    return add(multiplyScalar(c1, 1 - w), multiplyScalar(c2, w));
  }

  double brightness() {
    int r = (int) Math.floor(red * 255);
    int g = (int) Math.floor(green * 255);
    int b = (int) Math.floor(blue * 255);
    return (r * 77 + g * 150 + b * 29) >> 8;
  }

  @JsNonNull
  @Override
  public String toString() {
    double r = Math.floor(red * 255);
    double g = Math.floor(green * 255);
    double b = Math.floor(blue * 255);

    return "rgb(" + r + "," + g + "," + b + ")";
  }
}
