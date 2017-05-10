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
package com.google.j2cl.transpiler.integration.objectdevirtualcalls;

/**
 * Verifies that Object methods on the Object class execute properly. It is effectively a test that
 * Object method devirtualization is occurring and that the implementation being routed to is
 * proper.
 */
public class Main {
  public static void main(String... args) {
    Object object1 = new Object();
    Object object2 = new Object();

    // Equals
    assert object1.equals(object1);
    assert !object1.equals(object2);
    assert !object1.equals("some string");

    // HashCode
    assert object1.hashCode() != -1;
    assert object1.hashCode() == object1.hashCode();
    assert object1.hashCode() != object2.hashCode();

    // ToString
    assert object1.toString() instanceof String;
    assert object1.toString() == "java.lang.Object@" + Integer.toHexString(object1.hashCode());
    assert object1.toString() != object2.toString();

    // GetClass
    assert object1.getClass() instanceof Class;
    assert object1.getClass() == object2.getClass();
  }
}
