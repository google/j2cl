/*
 * Copyright 2025 Google Inc.
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
package j2ktnotpassing;

public class VariableHoisting {
  // Repro for b/470210135.
  public static void incorrectHoisting() {
    int i = 0;
    switch (i) {
      case 1:
        int j = 1;
      case 2:
        j = 2;
    }

    // local class
    class A {}

    switch (i) {
      case 1:
        A a = new A();
      case 2:
        a = null;
    }
  }
}
