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
package localclasswithconstructor;

public class LocalClassWithConstructor {
  public void test(final int p) {
    final int localVar = 1;
    // local class with non-default constructor and this() call
    class LocalClass {
      public int field;

      public LocalClass(int a, int b) {
        field = localVar + a + b;
      }

      public LocalClass(int a) {
        this(a, p);
        field = localVar;
      }
    }
    new LocalClass(1);
  }
}
