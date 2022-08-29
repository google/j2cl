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
package equality;

public class Equality {
  public void test() {
    // Primitives
    {
      boolean a = false == false;
      boolean b = 0 != 1;
    }

    // Objects
    {
      boolean c = new Object() != new Object();
    }

    // Double
    {
      boolean d = (Double) 0.0 == (Double) 0.0;
    }

    // Float
    {
      boolean e = (Float) 0.0f == (Float) 0.0f;
    }

    // Null literals
    {
      boolean f = null != new Object();
      boolean g = new Object() != null;
      boolean h = null != new int[0];
    }
  }
}
