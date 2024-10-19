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
package com.google.j2cl.benchmarks.octane.navierstokes;

class FluidField {

  private static final double DT = 0.1;

  private int iterations = 10;
  private double[] dens;
  private double[] densPrev;
  private double[] u;
  private double[] uPrev;
  private double[] v;
  private double[] vPrev;
  private int width;
  private int height;
  private int rowSize;
  private int size;
  private UICallback uiCallback;
  private DisplayFunction displayFunc;

  FluidField() {
    setResolution(64, 64);
  }

  private void addFields(double[] x, double[] s, double dt) {
    for (int i = 0; i < size; i++) {
      x[i] += dt * s[i];
    }
  }

  private void setBnd(int b, double[] x) {
    if (b == 1) {
      for (int i = 1; i <= width; i++) {
        x[i] = x[i + rowSize];
        x[i + (height + 1) * rowSize] = x[i + height * rowSize];
      }

      for (int j = 1; j <= height; j++) {
        x[j * rowSize] = -x[1 + j * rowSize];
        x[(width + 1) + j * rowSize] = -x[width + j * rowSize];
      }
    } else if (b == 2) {
      for (int i = 1; i <= width; i++) {
        x[i] = -x[i + rowSize];
        x[i + (height + 1) * rowSize] = -x[i + height * rowSize];
      }

      for (int j = 1; j <= height; j++) {
        x[j * rowSize] = x[1 + j * rowSize];
        x[(width + 1) + j * rowSize] = x[width + j * rowSize];
      }
    } else {
      for (int i = 1; i <= width; i++) {
        x[i] = x[i + rowSize];
        x[i + (height + 1) * rowSize] = x[i + height * rowSize];
      }

      for (int j = 1; j <= height; j++) {
        x[j * rowSize] = x[1 + j * rowSize];
        x[(width + 1) + j * rowSize] = x[width + j * rowSize];
      }
    }
    int maxEdge = (height + 1) * rowSize;
    x[0] = 0.5 * (x[1] + x[rowSize]);
    x[maxEdge] = 0.5 * (x[1 + maxEdge] + x[height * rowSize]);
    x[(width + 1)] = 0.5 * (x[width] + x[(width + 1) + rowSize]);
    x[(width + 1) + maxEdge] = 0.5 * (x[width + maxEdge] + x[(width + 1) + height * rowSize]);
  }

  private void linSolve(int b, double[] x, double[] x0, int a, int c) {
    if (a == 0 && c == 1) {
      for (int j = 1; j <= height; j++) {
        int currentRow = j * rowSize;
        ++currentRow;
        for (int i = 0; i < width; i++) {
          x[currentRow] = x0[currentRow];
          ++currentRow;
        }
      }
      setBnd(b, x);
    } else {
      double invC = 1. / c;
      for (int k = 0; k < iterations; k++) {
        for (int j = 1; j <= height; j++) {
          int lastRow = (j - 1) * rowSize;
          int currentRow = j * rowSize;
          int nextRow = (j + 1) * rowSize;
          double lastX = x[currentRow];
          ++currentRow;
          for (int i = 1; i <= width; i++) {
            lastX =
                x[currentRow] =
                    (x0[currentRow] + a * (lastX + x[++currentRow] + x[++lastRow] + x[++nextRow]))
                        * invC;
          }
        }
        setBnd(b, x);
      }
    }
  }

  private void diffuse(int b, double[] x, double[] x0, @SuppressWarnings("unused") double dt) {
    int a = 0;
    linSolve(b, x, x0, a, 1 + 4 * a);
  }

  private void linSolve2(double[] x, double[] x0, double[] y, double[] y0, int a, int c) {
    if (a == 0 && c == 1) {
      for (int j = 1; j <= height; j++) {
        int currentRow = j * rowSize;
        ++currentRow;
        for (int i = 0; i < width; i++) {
          x[currentRow] = x0[currentRow];
          y[currentRow] = y0[currentRow];
          ++currentRow;
        }
      }
      setBnd(1, x);
      setBnd(2, y);
    } else {
      double invC = 1. / c;
      for (int k = 0; k < iterations; k++) {
        for (int j = 1; j <= height; j++) {
          int lastRow = (j - 1) * rowSize;
          int currentRow = j * rowSize;
          int nextRow = (j + 1) * rowSize;
          double lastX = x[currentRow];
          double lastY = y[currentRow];
          ++currentRow;
          for (int i = 1; i <= width; i++) {
            lastX =
                x[currentRow] =
                    (x0[currentRow] + a * (lastX + x[currentRow] + x[lastRow] + x[nextRow])) * invC;
            lastY =
                y[currentRow] =
                    (y0[currentRow] + a * (lastY + y[++currentRow] + y[++lastRow] + y[++nextRow]))
                        * invC;
          }
        }
        setBnd(1, x);
        setBnd(2, y);
      }
    }
  }

  private void diffuse2(
      double[] x, double[] x0, double[] y, double[] y0, @SuppressWarnings("unused") double dt) {
    int a = 0;
    linSolve2(x, x0, y, y0, a, 1 + 4 * a);
  }

  private void advect(int b, double[] d, double[] d0, double[] u, double[] v, double dt) {
    double wdt0 = dt * width;
    double hdt0 = dt * height;
    double wp5 = width + 0.5;
    double hp5 = height + 0.5;
    for (int j = 1; j <= height; j++) {
      int pos = j * rowSize;
      for (int i = 1; i <= width; i++) {
        double x = i - wdt0 * u[++pos];
        double y = j - hdt0 * v[pos];
        if (x < 0.5) {
          x = 0.5;
        } else if (x > wp5) {
          x = wp5;
        }
        int i0 = (int) x;
        int i1 = i0 + 1;
        if (y < 0.5) {
          y = 0.5;
        } else if (y > hp5) {
          y = hp5;
        }
        int j0 = (int) y;
        int j1 = j0 + 1;
        double s1 = x - i0;
        double s0 = 1 - s1;
        double t1 = y - j0;
        double t0 = 1 - t1;
        int row1 = j0 * rowSize;
        int row2 = j1 * rowSize;
        d[pos] =
            s0 * (t0 * d0[i0 + row1] + t1 * d0[i0 + row2])
                + s1 * (t0 * d0[i1 + row1] + t1 * d0[i1 + row2]);
      }
    }
    setBnd(b, d);
  }

  private void project(double[] u, double[] v, double[] p, double[] div) {
    double h = -0.5 / Math.sqrt(width * height);
    for (int j = 1; j <= height; j++) {
      int row = j * rowSize;
      int previousRow = (j - 1) * rowSize;
      int prevValue = row - 1;
      int currentRow = row;
      int nextValue = row + 1;
      int nextRow = (j + 1) * rowSize;
      for (int i = 1; i <= width; i++) {
        div[++currentRow] = h * (u[++nextValue] - u[++prevValue] + v[++nextRow] - v[++previousRow]);
        p[currentRow] = 0;
      }
    }
    setBnd(0, div);
    setBnd(0, p);

    linSolve(0, p, div, 1, 4);
    double wScale = 0.5 * width;
    double hScale = 0.5 * height;
    for (int j = 1; j <= height; j++) {
      int prevPos = j * rowSize - 1;
      int currentPos = j * rowSize;
      int nextPos = j * rowSize + 1;
      int prevRow = (j - 1) * rowSize;
      @SuppressWarnings("unused")
      int currentRow = j * rowSize;
      int nextRow = (j + 1) * rowSize;

      for (int i = 1; i <= width; i++) {
        u[++currentPos] -= wScale * (p[++nextPos] - p[++prevPos]);
        v[currentPos] -= hScale * (p[++nextRow] - p[++prevRow]);
      }
    }
    setBnd(1, u);
    setBnd(2, v);
  }

  private void densStep(double[] x, double[] x0, double[] u, double[] v, double dt) {
    addFields(x, x0, dt);
    diffuse(0, x0, x, dt);
    advect(0, x, x0, u, v, dt);
  }

  private void velStep(double[] u, double[] v, double[] u0, double[] v0, double dt) {
    addFields(u, u0, dt);
    addFields(v, v0, dt);
    double[] temp = u0;
    u0 = u;
    u = temp;
    temp = v0;
    v0 = v;
    v = temp;
    diffuse2(u, u0, v, v0, dt);
    project(u, v, u0, v0);
    temp = u0;
    u0 = u;
    u = temp;
    temp = v0;
    v0 = v;
    v = temp;
    advect(1, u, u0, u0, v0, dt);
    advect(2, v, v0, u0, v0, dt);
    project(u, v, u0, v0);
  }

  // var displayFunc;

  private void queryUI(double[] d, double[] u, double[] v) {
    for (int i = 0; i < size; i++) {
      u[i] = v[i] = d[i] = 0.0;
    }

    uiCallback.prepareFrame(new Field(d, u, v, rowSize));
  }

  void update() {
    queryUI(densPrev, uPrev, vPrev);
    velStep(u, v, uPrev, vPrev, DT);
    densStep(dens, densPrev, u, v, DT);
    displayFunc.call(new Field(dens, u, v, rowSize));
  }

  interface DisplayFunction {
    void call(Field f);
  }

  void setDisplayFunction(DisplayFunction func) {
    displayFunc = func;
  }

  void setIterations(int iters) {
    if (iters > 0 && iters <= 100) {
      iterations = iters;
    }
  }

  interface UICallback {
    void prepareFrame(Field field);
  }

  void setUICallback(UICallback callback) {
    uiCallback = callback;
  }

  void reset() {
    rowSize = width + 2;
    size = (width + 2) * (height + 2);
    dens = new double[size];
    densPrev = new double[size];
    u = new double[size];
    uPrev = new double[size];
    v = new double[size];
    vPrev = new double[size];
  }

  double[] getDens() {
    return dens;
  }

  boolean setResolution(int hRes, int wRes) {
    int res = wRes * hRes;
    if (res > 0 && res < 1000000 && (wRes != width || hRes != height)) {
      width = wRes;
      height = hRes;
      reset();
      return true;
    }
    return false;
  }
}
