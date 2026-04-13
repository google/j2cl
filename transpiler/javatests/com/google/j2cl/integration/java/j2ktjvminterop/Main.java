/*
 * Copyright 2026 Google Inc.
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
package j2ktjvminterop;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import com.google.j2cl.integration.testing.TestUtils;

@SuppressWarnings("BadInstanceof") // Needed for Asserts.assertTrue import.
public class Main {
  public static void main(String... args) {
    testProtectedInParentPublicInChild();
    testRecordConvertedToDataClass();
    testRecordNotConvertedToDataClass();
  }

  private static void testProtectedInParentPublicInChild() {
    KotlinChild kotlinChild = new KotlinChild();
    // TODO(b/489150132): Uncomment when J2KT transpires explicit visibility.
    // kotlinChild.protectedInParentPublicInChild();
    kotlinChild.publicInChildDelegatingToProtectedInParent();
  }

  private static void testRecordConvertedToDataClass() {
    RecordConvertedToDataClass record = new RecordConvertedToDataClass(123, "a");
    if (!TestUtils.isJ2Kt()) {
      assertTrue(record instanceof Record);
    } else {
      // TODO(b/445545563): Should be true for non-native once Java records are translated to
      // Kotlin data classes with @JvmRecord annotation.
      assertFalse(record instanceof Record);
    }

    assertEquals(123, record.a());
    assertEquals("a", record.b());
  }

  private static void testRecordNotConvertedToDataClass() {
    RecordNotConvertedToDataClass record = new RecordNotConvertedToDataClass(123, "a");
    if (!TestUtils.isJ2Kt()) {
      assertTrue(record instanceof Record);
    } else {
      // Records which are not converted to data classes are not instances of Record.
      assertFalse(record instanceof Record);
    }

    assertEquals(123, record.a());
    assertEquals("a", record.b());
  }
}
