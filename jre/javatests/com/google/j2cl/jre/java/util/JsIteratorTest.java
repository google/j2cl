/*
 * Copyright 2026 Google Inc.
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
package com.google.j2cl.jre.java.util;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import javaemul.internal.JsIterableHelper.JsIterable;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import junit.framework.TestCase;

/** Testing JS contract of Iterator interface. */
public class JsIteratorTest extends TestCase {

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Array.from")
  private static native String[] arrayFrom(JsIterable<String> jsIterable);
}
