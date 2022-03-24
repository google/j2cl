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
package objectdevirtualcalls;

import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Verifies that Object methods on the Object class execute properly. It is effectively a test that
 * Object method devirtualization is occurring and that the implementation being routed to is
 * proper.
 */
public class Main {
  public static void main(String... args) {
    testEquals();
    testHashCode();
    testToString();
    testGetClass();
  }

  private static Object object1 = new Object();
  private static Object object2 = new Object();

  private static void testEquals() {
    assertTrue(object1.equals(object1));
    assertTrue(!object1.equals(object2));
    assertTrue(!object1.equals("some string"));
  }

  private static void testHashCode() {
    assertTrue(object1.hashCode() != -1);
    assertTrue(object1.hashCode() == object1.hashCode());
    assertTrue(object1.hashCode() != object2.hashCode());
  }

  private static void testToString() {
    assertTrue(
        object1.toString().equals("java.lang.Object@" + Integer.toHexString(object1.hashCode())));
    assertFalse(object1.toString().equals(object2.toString()));
  }

  private static void testGetClass() {
    assertTrue(object1.getClass() instanceof Class);
    assertTrue(object1.getClass() == object2.getClass());
  }
}
