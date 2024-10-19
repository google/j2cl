/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.benchmarks.octane.raytrace;

import static java.lang.Math.max;

class Engine {

  static class Options {
    int canvasHeight = 100;
    int canvasWidth = 100;
    int pixelWidth = 2;
    int pixelHeight = 2;
    boolean renderDiffuse = false;
    boolean renderShadows = false;
    boolean renderHighlights = false;
    boolean renderReflections = false;
    int rayDepth = 2;
  }

  private final Options options;

  Engine(Options options) {
    this.options = options;
    this.options.canvasHeight /= this.options.pixelHeight;
    this.options.canvasWidth /= this.options.pixelWidth;
  }

  @SuppressWarnings("unused")
  void setPixel(double x, double y, Color color) {
    if (x == y) {
      RayTrace.checkNumber = (int) (RayTrace.checkNumber + color.brightness());
    }
  }

  void renderScene(Scene scene) {
    double canvasHeight = options.canvasHeight;
    double canvasWidth = options.canvasWidth;

    for (int y = 0; y < canvasHeight; y++) {
      for (int x = 0; x < canvasWidth; x++) {
        double yp = y * 1.0 / canvasHeight * 2 - 1;
        double xp = x * 1.0 / canvasWidth * 2 - 1;

        Ray ray = scene.getCamera().getRay(xp, yp);

        Color color = getPixelColor(ray, scene);

        setPixel(x, y, color);
      }
    }
  }

  Color getPixelColor(Ray ray, Scene scene) {
    IntersectionInfo info = testIntersection(ray, scene, null);
    if (info.isHit()) {
      return rayTrace(info, ray, scene, 0);
    }
    return scene.getBackground().getColor();
  }

  IntersectionInfo testIntersection(Ray ray, Scene scene, Shape exclude) {
    IntersectionInfo best = new IntersectionInfo();
    best.setDistance(2000);

    for (int i = 0; i < scene.getShapes().length; i++) {
      Shape shape = scene.getShape(i);

      if (shape != exclude) {
        IntersectionInfo info = shape.intersect(ray);
        if (info.isHit() && info.getDistance() >= 0 && info.getDistance() < best.getDistance()) {
          best = info;
        }
      }
    }
    return best;
  }

  Ray getReflectionRay(Vector p, Vector n, Vector v) {
    double c1 = -n.dot(v);
    Vector r1 = Vector.add(Vector.multiplyScalar(n, 2 * c1), v);
    return new Ray(p, r1);
  }

  Color rayTrace(IntersectionInfo info, Ray ray, Scene scene, double depth) {
    // Calc ambient
    Color color = Color.multiplyScalar(info.getColor(), scene.getBackground().getAmbience());
    @SuppressWarnings("unused")
    Color oldColor = color;
    double shininess = Math.pow(10, info.getShape().getMaterial().gloss + 1);

    for (int i = 0; i < scene.getLights().length; i++) {
      Light light = scene.getLight(i);

      // Calc diffuse lighting
      Vector v = Vector.subtract(light.getPosition(), info.getPosition()).normalize();

      if (options.renderDiffuse) {
        double l = v.dot(info.getNormal());
        if (l > 0.0) {
          color =
              Color.add(
                  color,
                  Color.multiply(info.getColor(), Color.multiplyScalar(light.getColor(), l)));
        }
      }

      // The greater the depth the more accurate the colours, but
      // this is exponentially (!) expensive
      if (depth <= options.rayDepth) {
        // calculate reflection ray
        if (options.renderReflections && info.getShape().getMaterial().reflection > 0) {
          Ray reflectionRay =
              getReflectionRay(info.getPosition(), info.getNormal(), ray.getDirection());
          IntersectionInfo refl = testIntersection(reflectionRay, scene, info.getShape());

          if (refl.isHit() && refl.getDistance() > 0) {
            refl.setColor(rayTrace(refl, reflectionRay, scene, depth + 1));
          } else {
            refl.setColor(scene.getBackground().getColor());
          }

          color = Color.blend(color, refl.getColor(), info.getShape().getMaterial().reflection);
        }
      }

      /* Render shadows and highlights */

      IntersectionInfo shadowInfo = new IntersectionInfo();

      if (options.renderShadows) {
        Ray shadowRay = new Ray(info.getPosition(), v);

        shadowInfo = testIntersection(shadowRay, scene, info.getShape());
        if (shadowInfo.isHit() && shadowInfo.getShape() != info.getShape()) {
          Color vA = Color.multiplyScalar(color, 0.5);
          double dB = (0.5 * Math.sqrt(shadowInfo.getShape().getMaterial().transparency));
          color = Color.addScalar(vA, dB);
        }
      }

      // Phong specular highlights
      if (options.renderHighlights
          && !shadowInfo.isHit()
          && info.getShape().getMaterial().gloss > 0) {
        Vector lv = Vector.subtract(info.getShape().getPosition(), light.getPosition()).normalize();

        Vector e =
            Vector.subtract(scene.getCamera().getPosition(), info.getShape().getPosition())
                .normalize();

        Vector h = Vector.subtract(e, lv).normalize();

        double glossWeight = Math.pow(max(info.getNormal().dot(h), 0), shininess);
        color = Color.add(Color.multiplyScalar(light.getColor(), glossWeight), color);
      }
    }
    color.limit();
    return color;
  }
}
