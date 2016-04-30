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
package com.google.j2cl.tools.integration.innerclasses;

import junit.framework.TestCase;

/**
 * Test that native methods (both instance and static methods) are converted and added to the right
 * prototype.
 */
public class Test extends TestCase {

  public void testInnerInnerStaticMethod() {
    assertEquals("staticInnerInner", Outer.Inner.InnerInner.staticInnerInner());
  }

  public void testInnerInnerClassMethod() {
    Outer.Inner.InnerInner instance = new Outer.Inner.InnerInner();
    assertEquals("innerInner", instance.innerInner());
  }

  public void testInnerStaticMethod() {
    assertEquals("staticInner", Outer.Inner.staticInner());
  }

  public void testInnerClassMethod() {
    Outer.Inner instance = new Outer.Inner();
    assertEquals("inner", instance.inner());
  }
}
