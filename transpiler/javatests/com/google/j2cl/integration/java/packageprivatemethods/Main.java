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
package packageprivatemethods;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;

import packageprivatemethods.a.M1;
import packageprivatemethods.a.M3;
import packageprivatemethods.a.N1;
import packageprivatemethods.a.N3;
import packageprivatemethods.a.O1;
import packageprivatemethods.a.O2;
import packageprivatemethods.a.O3;
import packageprivatemethods.a.P1;
import packageprivatemethods.a.P3;
import packageprivatemethods.b.M2;
import packageprivatemethods.b.M4;
import packageprivatemethods.b.N2;
import packageprivatemethods.b.N4;
import packageprivatemethods.b.P2;
import packageprivatemethods.b.P4;
import packageprivatemethods.package1.Caller;

public class Main {
  public static void main(String[] args) {
    testSimple();
    testCrossPackageOverrides_independentOverrideChains();
    testCrossPackageOverrides_gradualPublicOverrides();
    testCrossPackageOverrides_intermediateDummyClass();
    testCrossPackageOverrides_gradualWithIntermediateDummyClass();
  }

  private static void testSimple() {
    new Caller().test();
  }

  private static void testCrossPackageOverrides_independentOverrideChains() {
    // This is the pattern
    //
    // package a                                   package b
    // class M1              { m() {} }
    //                          |
    //                          V                 class M2 extends M1 { m() {} }
    // class M3 extends M2   { m() {} }                                   |
    //                                                                    V
    //                                             class M4 extends M3 { m() {} }

    // calls through m_a
    assertEquals("a.M1.m", M1.callM_a(new M1()));
    assertEquals("a.M1.m", M1.callM_a(new M2()));
    assertEquals("a.M3.m", M1.callM_a(new M3()));
    // javac/jvm incorrectly dispatches this one b.M4.m but b.M4.m does NOT override a.M1.m.
    assertEquals("a.M3.m", M1.callM_a(new M4()));

    // calls through m_b
    assertEquals("b.M2.m", M2.callM_b(new M2()));
    assertEquals("b.M2.m", M2.callM_b(new M3()));
    assertEquals("b.M4.m", M2.callM_b(new M4()));
  }

  private static void testCrossPackageOverrides_gradualPublicOverrides() {
    // This is the pattern
    //
    // package a                                   package b
    // class N1              { m() {} }
    //                          |
    //                          V                 class N2 extends N1 { m() {} }
    // class N3 extends N2   {public m() {} }                             |
    //                           |                                        V
    //                           \---------------> class N4 extends N3 { public m() {} }

    // calls through m_a
    assertEquals("a.N1.m", N1.callM_a(new N1()));
    assertEquals("a.N1.m", N1.callM_a(new N2()));
    assertEquals("a.N3.m", N1.callM_a(new N3()));
    assertEquals("b.N4.m", N1.callM_a(new N4()));

    // calls through m_b
    assertEquals("b.N2.m", N2.callM_b(new N2()));
    assertEquals("b.N2.m", N2.callM_b(new N3()));
    assertEquals("b.N4.m", N2.callM_b(new N4()));
  }

  private static void testCrossPackageOverrides_intermediateDummyClass() {
    // This is the pattern
    //
    // package a
    // class O1              { m() {} }
    //                          |
    //                          |
    // class O2 extends O1   {} |
    //                          |
    //                          V
    // class O3 extends O2   {public m() {} }

    // calls through m_a
    assertEquals("a.O1.m", O1.callM_a(new O1()));
    assertEquals("a.O1.m", O1.callM_a(new O2()));
    assertEquals("a.O3.m", O1.callM_a(new O3()));
  }

  private static void testCrossPackageOverrides_gradualWithIntermediateDummyClass() {
    // This is the pattern
    //
    // package a                                     package b
    // class P1              { m() {} }
    //                          |
    //                          V                    class P2 extends P1 { m() {} }
    // class P3 extends P2   {public m() {} }                             |
    //                           |                                        V
    //                           \---------------> ? class P4 extends P3 {}
    //                      (does P3.m() override P2.m()?)
    //
    // Our interpretation of the Java spec is that P3.m does not accidentally override P2.m at P4.

    // calls through m_a
    assertEquals("a.P1.m", P1.callM_a(new P1()));
    assertEquals("a.P1.m", P1.callM_a(new P2()));
    assertEquals("a.P3.m", P1.callM_a(new P3()));
    assertEquals("a.P3.m", P1.callM_a(new P4()));

    // calls through m_b
    assertEquals("b.P2.m", P2.callM_b(new P2()));
    assertEquals("b.P2.m", P2.callM_b(new P3()));
    assertEquals("b.P2.m", P2.callM_b(new P4()));
  }
}
