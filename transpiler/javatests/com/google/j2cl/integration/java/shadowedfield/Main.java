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
package shadowedfield;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test shadowed field.
 */
public class Main {
  public static void main(String... args) {
    Child instance = new Child();

    // Value starts uninitialized.
    assertTrue(instance.foo == 0);

    ((Parent) instance).foo = 10;
    // Changing the foo field value in the ParentClass has no effect on the field when read out of
    // the ChildClass, because it's shadowed.
    assertTrue(instance.foo == 0);
    // But explicitly reading the field from the ParentClass will show the value change.
    assertTrue(((Parent) instance).foo == 10);
    // Changing the value in the shadowing field is visible when also reading from that shadowing
    // field.
    instance.foo = 20;
    assertTrue(instance.foo == 20);
  }
}
