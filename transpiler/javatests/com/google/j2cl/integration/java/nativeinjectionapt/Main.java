/*
 * Copyright 2016 Google Inc.
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
package nativeinjectionapt;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/** Integration tests for native methods written by APTs. */
public class Main {
  public static void main(String... args) {
    assertTrue(MyClass.nativeStaticMethod().equals("MyClass"));
    assertTrue(NativeClass.nativeStaticMethod().equals("NativeClass"));
    assertTrue(SuperMyClass.nativeStaticMethod().equals("SuperMyClass"));
  }
}
