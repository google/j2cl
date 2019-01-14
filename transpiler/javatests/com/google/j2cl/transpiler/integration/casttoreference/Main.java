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
package com.google.j2cl.transpiler.integration.casttoreference;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

public class Main {
  public static void main(String... args) {
    testPrimitiveToReference();
  }

  @SuppressWarnings("cast")
  public static void testPrimitiveToReference() {
    boolean bool = true;
    byte b = 1;
    char c = 'a';
    short s = 1;
    int i = 1;
    long l = 1L;
    float f = 1.0f;
    double d = 1.0;
    Object o = bool;
    assertTrue(o != null);
    o = b;
    assertTrue(o != null);
    o = c;
    assertTrue(o != null);
    o = s;
    assertTrue(o != null);
    o = i;
    assertTrue(o != null);
    o = l;
    assertTrue(o != null);
    o = f;
    assertTrue(o != null);
    o = d;
    assertTrue(o != null);
    o = (Object) bool;
    assertTrue(o != null);
    o = (Object) b;
    assertTrue(o != null);
    o = (Object) c;
    assertTrue(o != null);
    o = (Object) s;
    assertTrue(o != null);
    o = (Object) i;
    assertTrue(o != null);
    o = (Object) l;
    assertTrue(o != null);
    o = (Object) f;
    assertTrue(o != null);
    o = (Object) d;
    assertTrue(o != null);
  }
}
