/*
 * Copyright 2019 Google Inc.
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

import junit.framework.TestCase;

/** Tests equals semantics for Float. */
public final class FloatEqualsSemanticsTest extends TestCase {

  public void testEquals() {
    assertTrue(Float.valueOf(Float.NaN).equals(Float.NaN));
    assertFalse(Float.valueOf(0.0f).equals(-0.0f));
  }
}
