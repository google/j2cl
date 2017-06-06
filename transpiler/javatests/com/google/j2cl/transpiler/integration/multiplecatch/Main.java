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
package com.google.j2cl.transpiler.integration.multiplecatch;

/**
 * Test multiple catch.
 */
public class Main {
  public static void main(String... args) {
    try {
      throwRuntimeException(); // So assert isn't considered dead code by javac.
      assert false; // should have skipped past this with an exception.
    } catch (NullPointerException | ClassCastException e) {
      assert false; // The wrong exception type was caught.
    } catch (RuntimeException r) {
      // expected
    }

    try {
      throwIllegalArgumentException(); // So assert isn't considered dead code by javac.
      assert false; // should have skipped past this with an exception.
    } catch (NullPointerException | IllegalArgumentException e) {
      // expected
    }
  }

  public static void throwRuntimeException() {
    throw new RuntimeException();
  }

  public static void throwIllegalArgumentException() {
    throw new IllegalArgumentException();
  }
}
