/*
 * Copyright 2013 Google Inc.
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

import java.lang.reflect.Type;
import junit.framework.TestCase;

/** This is the test-case for {@link java.lang.reflect.Type} and {@link Class}. */
public class TypeTest extends TestCase {

  /** Tests that {@link Class} actually implements {@link java.lang.reflect.Type}. */
  public void testClassImplementsType() {
    assertTrue(Object.class instanceof Type);
  }
}
