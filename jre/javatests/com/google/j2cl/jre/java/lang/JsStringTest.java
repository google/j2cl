/*
 * Copyright 2022 Google Inc.
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
package com.google.j2cl.jre.java.lang;

import com.google.gwt.junit.client.GWTTestCase;

/** Tests java.lang.String. */
public final class JsStringTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  // String.intern is not available in j2wasm.
  public void testIntern() {
    String s1 = String.valueOf(new char[] {'a', 'b', 'c', 'd', 'e', 'f'});
    String s2 = String.valueOf(new char[] {'a', 'b', 'c', 'd', 'e', 'f'});
    assertTrue("strings not equal", s1.equals(s2));
    assertSame("interns are not the same reference", s1.intern(), s2.intern());

    String nullString = null;
    try {
      String ignored = nullString.intern();
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }
}
