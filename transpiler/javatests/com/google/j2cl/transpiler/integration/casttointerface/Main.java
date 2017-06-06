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
package com.google.j2cl.transpiler.integration.casttointerface;

import java.io.Serializable;

/**
 * Test cast to interface type.
 */
public class Main {
  @SuppressWarnings("unused")
  public static void main(String[] args) {
    Object object = new Person();

    // This is fine.
    HasName hasName = (HasName) object;

    // But this isn't fine.
    try {
      Serializable exception = (Serializable) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }
}
