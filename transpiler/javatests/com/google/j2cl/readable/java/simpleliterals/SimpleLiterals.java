/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package simpleliterals;

public class SimpleLiterals {
  @SuppressWarnings("unused")
  public void main() {
    boolean bool = false;
    char ch = 'a';
    byte b = 127;
    byte nb = -128;
    short s = 32767;
    short ns = -32768;
    int i = 101;
    int ni = -101;
    long l = 101L;
    long nl = -101L;
    float f = 101.0f;
    float nf = -101.0f;
    float zf = 0.0f;
    float nzf = -0.0f;
    double d = 101.0;
    double nd = -101.0;
    double zd = 0.0;
    double nzd = -0.0;
    Object o = null;
    String str = "foo";
    Class<?> c = SimpleLiterals.class;
    float zeroF = -0.0f;
    float minusZeroF = -0.0f;
    double zeroD = 0.0;
    double minusZeroD = -0.0;
    double minusMinusZeroD = - -0.0;
  }
}
