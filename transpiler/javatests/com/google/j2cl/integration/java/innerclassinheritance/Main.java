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
package innerclassinheritance;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void main(String[] args) {
    innerclassinheritance.p1.A a =
        new innerclassinheritance.p1.A();
    innerclassinheritance.p1.A aa =
        new innerclassinheritance.p2.A();
    assertTrue((a.new B() instanceof innerclassinheritance.p1.A.B));

    assertTrue((aa.new B() instanceof innerclassinheritance.p1.A.B));

    assertTrue(!(aa.new B() instanceof innerclassinheritance.p2.A.B));
    assertTrue(
        (new innerclassinheritance.p2.A().new B()
            instanceof innerclassinheritance.p2.A.B));
    assertTrue(
        (new innerclassinheritance.p2.A().new C()
            instanceof innerclassinheritance.p1.A.C));
  }
}
