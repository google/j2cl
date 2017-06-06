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
package com.google.j2cl.transpiler.integration.casttoclassdisabledresultused;

/**
 * Test disabling of runtime cast checking but also using the value afterward.
 */
public class Main {
  @SuppressWarnings("unused")
  public static void main(String[] args) {
    Object object = new Main();

    // This should fail! But we've turned off cast checking in the BUILD file.
    RuntimeException exception = (RuntimeException) object;

    // Use the resulting casted object, to prove that size reductions are not accidentally because
    // the object was unused and JSCompiler decided to delete the cast on an unused thing.
    if (exception instanceof Object) {
      // Do something that has a side effect
      try {
        throw new RuntimeException(exception.toString());
      } catch (RuntimeException e) {
        // Good catch!11!!
      }
    }
  }
}
