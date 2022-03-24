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
package nestednativetype;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {

  /**
   * This proves there is no error by showing that no JSCompiler compile error occurred. If we
   * improperly create the qualified names of types nested inside of native types then we will have
   * created a name collision.
   */
  public static void main(String... args) {
    NativeArray nativeArray = new NativeArray();
    NativeArray2 nativeArray2 = new NativeArray2();

    assertTrue(nativeArray.getObject() != nativeArray2.getObject());
  }
}
