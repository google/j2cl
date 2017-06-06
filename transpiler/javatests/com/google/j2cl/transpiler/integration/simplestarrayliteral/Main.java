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
package com.google.j2cl.transpiler.integration.simplestarrayliteral;

public class Main {
  @SuppressWarnings("cast")
  public static void main(String... args) {
    // This translates into a raw JS array with no custom Java initialization.
    Object[] values = new Object[] {"sam", "bob", "charlie"};

    // So of course you can insert a leaf value of a different but conforming type.
    values[0] = "jimmy";

    // But even so, you can not insert to an out of bounds index.
    try {
      values[100] = "ted";
      assert false : "An expected failure did not occur.";
    } catch (ArrayIndexOutOfBoundsException e) {
      // expected
    }

    // And it still has a functioning class literal.
    assert Object[].class == values.getClass();
  }
}
