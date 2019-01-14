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
package com.google.j2cl.transpiler.integration.objectchilddevirtualcalls;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

/**
 * Verifies that Object methods on the ChildClass class execute properly. It is effectively a test
 * that Object method devirtualization is occurring even for children of Object and that the
 * implementation being routed to is proper.
 */
@SuppressWarnings({
  "SelfEquals",
  "IdentityBinaryExpression",
  "ReferenceEquality",
  "EqualsIncompatibleType"
})
public class Main {
  public static void main(String... args) {
    ChildClass childClass1 = new ChildClass();
    ChildClass childClass2 = new ChildClass();

    // Equals
    assertTrue(childClass1.equals(childClass1));
    assertTrue(!childClass1.equals(childClass2));
    assertTrue(!childClass1.equals("some string"));

    // HashCode
    assertTrue(childClass1.hashCode() != -1);
    assertTrue(childClass1.hashCode() == childClass1.hashCode());
    assertTrue(childClass1.hashCode() != childClass2.hashCode());

    // ToString
    assertTrue(childClass1.toString() instanceof String);
    assertTrue(
        childClass1.toString()
            == "com.google.j2cl.transpiler.integration.objectchilddevirtualcalls.ChildClass@"
                + Integer.toHexString(childClass1.hashCode()));
    assertTrue(childClass1.toString() != childClass2.toString());

    // GetClass
    assertTrue(childClass1.getClass() instanceof Class);
    assertTrue(childClass1.getClass() == childClass2.getClass());

    new ChildClassOverrides().test();
  }
}
