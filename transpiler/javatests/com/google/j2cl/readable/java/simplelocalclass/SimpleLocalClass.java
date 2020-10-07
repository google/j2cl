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
package simplelocalclass;

public class SimpleLocalClass {
  public void test(final int p) {
    final int localVar = 1;
    class InnerClass {
      public int fun() {
        return localVar + p; // captured local variable and parameter.
      }
    }
    new InnerClass().fun(); // new local class.

    // local class that has no captured variables.
    class InnerClassWithoutCaptures {}
    new InnerClassWithoutCaptures();
  }

  // Local class with same name.
  public void fun() {
    final int localVar = 1;
    class InnerClass {
      int field = localVar;
    }
  }

  // Local class with same name after $.
  public void foo() {
    class Abc$InnerClass {}
    class Klm$InnerClass {}
  }
}
