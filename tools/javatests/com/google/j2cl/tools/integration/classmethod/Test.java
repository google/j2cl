/*
 * Copyright 2015 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
package com.google.j2cl.tools.integration.classmethod;

import junit.framework.TestCase;

/**
 * Test that native methods (both instance and static method) are converted to executable javascript
 * functions.
 */
public class Test extends TestCase {
  public void testVoidNativeMethod() {
    ClassWithNativeMethod instance = new ClassWithNativeMethod();
    instance.nativeMethod("foo");
    assertEquals("foo", instance.field);
  }

  public void testNativeMethodWithResult() {
    ClassWithNativeMethod instance = new ClassWithNativeMethod();

    String result = instance.nativeMethodWithResult();
    assertEquals("nativeMethodWithResult", result);
  }

  public void testVoidStaticNativeMethod() {
    ClassWithNativeMethod.staticNative("foo");
    assertEquals("foo", ClassWithNativeMethod.staticField);
  }

  public void testStaticNativeMethodWithResult() {
    String result = ClassWithNativeMethod.staticNativeWithResult();
    assertEquals("staticNativeWithResult", result);
  }
}
