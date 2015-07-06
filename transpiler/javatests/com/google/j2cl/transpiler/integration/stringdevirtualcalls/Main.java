/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.integration.stringdevirtualcalls;

/**
 * Verifies that String methods on the String class execute properly. It is effectively a test that
 * String method devirtualization is occurring and that the implementation being routed to is
 * proper.
 */
public class Main {
  public static void main(String... args) {
    String string1 = "string1";
    String string2 = "string2";

    // Equals
    assert string1.equals(string1);
    assert !string1.equals(string2);
    assert !string1.equals(new Object());

    // HashCode
    assert string1.hashCode() != -1;
    assert string1.hashCode() == string1.hashCode();
    assert string1.hashCode() != string2.hashCode();

    // ToString
    assert string1.toString() != null;
    assert string1.toString() == string1;
    assert string1.toString() != string2.toString();

    // GetClass
    assert string1.getClass() instanceof Class;
    assert string1.getClass() == string2.getClass();
  }
}
