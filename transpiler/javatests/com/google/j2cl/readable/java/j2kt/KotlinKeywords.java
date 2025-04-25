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

import org.jspecify.annotations.NullMarked;

@NullMarked
public class KotlinKeywords {
  public int as = 0;
  public int fun = 0;
  public int in = 0;
  public int is = 0;
  public int object = 0;
  public int typealias = 0;
  public int val = 0;
  public int var = 0;
  public int when = 0;

  public int __ = 0;
  public int ___ = 0;

  public void test(int in) {
    int as = this.as + in;
    int fun = this.fun + in;
    int is = this.is + in;
    int object = this.object + in;
    int typealias = this.typealias + in;
    int val = this.val + in;
    int var = this.var + in;
    int when = this.when + in;

    int __ = this.__ + in;
    int ___ = this.___ + in;
  }
}
