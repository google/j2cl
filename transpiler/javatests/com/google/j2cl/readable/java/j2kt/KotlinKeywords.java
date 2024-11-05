/*
 * Copyright 2022 Google Inc.
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
package j2kt;

public class KotlinKeywords {
  public static int BIG_ENDIAN = 7;
  public static int LITTLE_ENDIAN = -7;
  public static int NULL = 745;
  public static int OVERFLOW = -24;
  public static int DOMAIN = -32;

  public int as = 0;
  public int delete = 0;
  public int fun = 0;
  public int in = 0;
  public int initialize = 0;
  public int is = 0;
  public int object = 0;
  public int scale = 0;
  public int typealias = 0;
  public int typeof = 0;
  public int val = 0;
  public int var = 0;
  public int when = 0;

  public int __ = 0;
  public int ___ = 0;

  public int test(int in) {
    int as = this.as + in;
    int delete = this.delete + in;
    int fun = this.fun + in;
    int initialize = this.initialize + in;
    int is = this.is + in;
    int object = this.object + in;
    int scale = this.scale + in;
    int typealias = this.typealias + in;
    int typeof = this.typeof + in;
    int val = this.val + in;
    int var = this.var + in;
    int when = this.when + in;

    int __ = this.__ + in;
    int ___ = this.___ + in;

    int temp = OVERFLOW + DOMAIN + in;

    return BIG_ENDIAN + LITTLE_ENDIAN + NULL + in;
  }
}
