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
package exceptionnotcaught;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test exception is not caught.
 */
public class Main {
  public static int value = 1;

  public static void main(String... args) {
    try {
      uncaughtException();
      value = 2; // exception should be thrown, this statement should not be executed.
      // assertTrue(false here cannot guarantee the transpiler transpiles correctly.
      // Even if it is not transpiled correctly, and the statement after uncaughtException() is
      // executed, it will be caught by the catch block and will not cause an error.
    } catch (ClassCastException e) {
      // expected.
    } finally {
      assertTrue(value == 1);
    }
  }

  public static void uncaughtException() throws ClassCastException {
    try {
      throw new ClassCastException();
    } catch (NullPointerException e) {
      assertTrue(false); // wrong exception type
    }
  }
}
