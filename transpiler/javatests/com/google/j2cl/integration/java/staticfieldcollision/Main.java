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
package staticfieldcollision;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsType;

/**
 * Tests that a name collision does not occur under the following circumstances:
 * - There is a static field with the same name as a generated method/field.
 * - That static field is not a compile time constant, requiring the generation of getters and a
 *   hidden field that contains the actual value.
 * - The static field is a @JsProperty.
 */
public class Main {
  @JsType
  public static class HasNameCollision {
    // Private storage should not collide with $clinit().
    public static Boolean clinit = new Boolean(false);
  }

  public static void main(String... args) {
    assertTrue(!HasNameCollision.clinit);
  }
}
