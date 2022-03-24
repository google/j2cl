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
package arrayobjectcalls;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

@SuppressWarnings({
  "ArrayEquals",
  "ArrayHashCode",
  "ArrayToString",
  "SelfEquals",
  "IdentityBinaryExpression",
  "ReferenceEquality"
})
public class Main {
  public static void main(String... args) {
    Main[] mains = new Main[1];
    Main[] other = new Main[1];

    assertTrue(mains.equals(mains));
    assertTrue(!mains.equals(other));
    assertTrue(mains.hashCode() == mains.hashCode());
    assertTrue(mains.hashCode() != other.hashCode());
    assertTrue(mains.getClass() == other.getClass());
  }
}
