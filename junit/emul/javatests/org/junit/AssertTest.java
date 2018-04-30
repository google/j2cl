/*
 * Copyright 2015 Google Inc.
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
package org.junit;

import java.util.Collections;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AssertTest {

  private final Object obj1 = Collections.nCopies(1, "data");
  private final Object obj2 = Collections.nCopies(2, "data");
  private final Object obj1Equal = Collections.nCopies(1, "data");

  @Test(expected = AssertionError.class)
  public void testFail() {
    Assert.fail("msg");
  }

  @Test
  public void testAssertEqualsDouble() {
    Assert.assertEquals(0.0, 0.0, 0.0);
    Assert.assertEquals(1.1, 1.1, 0.0);
    Assert.assertEquals(-1.1, -1.1, 0.0);
    Assert.assertEquals(Double.MIN_VALUE, Double.MIN_VALUE, 0.0);
    Assert.assertEquals(Double.MAX_VALUE, Double.MAX_VALUE, 0.0);
  }


  @Test
  public void testAssertEqualsDoubleFail() {
    testAssertEqualsDoubleFail(0.0, 0.00000000000000000000000000000000000000001, 0.0);
    testAssertEqualsDoubleFail(0.0, 0.0000000000000000001, 0.0);
    testAssertEqualsDoubleFail(0.0, 0.000000001, 0.0);
    testAssertEqualsDoubleFail(0.0, 0.0001, 0.0);
    testAssertEqualsDoubleFail(0.0, 0.1, 0.0);
    testAssertEqualsDoubleFail(1.0, 2.0, 0.1);
    testAssertEqualsDoubleFail(2.0, 1.0, 0.1);
    testAssertEqualsDoubleFail(-1.0, -2.0, 0.1);
    testAssertEqualsDoubleFail(-2.0, -1.0, 0.1);
  }

  private static void testAssertEqualsDoubleFail(double a, double b, double delta) {
    boolean failed = false;
    try {
      Assert.assertEquals(a, b, delta);
    } catch (AssertionError expected) {
      failed = true;
    }

    if (!failed) {
      Assert.fail("Expected failure for assertEquals(" + a + ", " + b + ", " + delta + ")");
    }
  }

  @Test
  public void testAssertEqualsFloat() {
    Assert.assertEquals(0.0f, 0.0f, 0.0f);
    Assert.assertEquals(1.1f, 1.1f, 0.0f);
    Assert.assertEquals(-1.1f, -1.1f, 0.0f);
    Assert.assertEquals(Float.MIN_VALUE, Float.MIN_VALUE, 0.0f);
    Assert.assertEquals(Float.MAX_VALUE, Float.MAX_VALUE, 0.0f);
  }

  @Test
  public void testAssertEqualsFloatFail() {
    testAssertEqualsFloatFail(0.0f, 0.00000000000000000000000000000000000000001f, 0.0f);
    testAssertEqualsFloatFail(0.0f, 0.0000000000000000001f, 0.0f);
    testAssertEqualsFloatFail(0.0f, 0.000000001f, 0.0f);
    testAssertEqualsFloatFail(0.0f, 0.0001f, 0.0f);
    testAssertEqualsFloatFail(0.0f, 0.1f, 0.0f);
    testAssertEqualsFloatFail(1.0f, 2.0f, 0.1f);
    testAssertEqualsFloatFail(2.0f, 1.0f, 0.1f);
    testAssertEqualsFloatFail(-1.0f, -2.0f, 0.1f);
    testAssertEqualsFloatFail(-2.0f, -1.0f, 0.1f);
  }

  private static void testAssertEqualsFloatFail(float a, float b, float delta) {
    boolean failed = false;
    try {
      Assert.assertEquals(a, b, delta);
    } catch (AssertionError expected) {
      failed = true;
    }

    if (!failed) {
      Assert.fail("Expected failure for assertEquals(" + a + ", " + b + ", " + delta + ")");
    }
  }

  @Test
  public void testAssertEqualsInt() {
    Assert.assertEquals(5, 5);
    Assert.assertEquals("should_never_be_in_the_log", 5, 5);
  }

  @Test(expected = AssertionError.class)
  public void testAssertEqualsIntFail() {
    Assert.assertEquals(5, 4);
  }

  @Test(expected = AssertionError.class)
  public void testAssertEqualsIntFailWithMessage() {
    Assert.assertEquals("msg", 5, 4);
  }

  @Test
  public void testAssertEqualsObject() {
    Assert.assertEquals(obj1, obj1Equal);
    Assert.assertEquals("should_never_be_in_the_log", obj1, obj1);
  }

  @Test(expected = AssertionError.class)
  public void testAssertEqualsObjectFail() {
    Assert.assertEquals(obj1, obj2);
  }

  @Test(expected = AssertionError.class)
  public void testAssertEqualsObjectFailWithMessage() {
    Assert.assertEquals("msg", obj1, obj2);
  }

  @Test
  public void testAssertNull() {
    Assert.assertNull(null);
    Assert.assertNull("should_never_be_in_the_log", null);
  }

  @Test(expected = AssertionError.class)
  public void testAssertNullFail() {
    Assert.assertNull("Hello");
  }

  @Test(expected = AssertionError.class)
  public void testAssertNullFailWithMessage() {
    Assert.assertNull("msg", "Hello");
  }

  @Test
  public void testAssertNotNull() {
    Assert.assertNotNull("Hello");
    Assert.assertNotNull("should_never_be_in_the_log", "Hello");
  }

  @Test(expected = AssertionError.class)
  public void testAssertNotNullFail() {
    Assert.assertNotNull(null);
  }

  @Test(expected = AssertionError.class)
  public void testAssertNotNullFailWithMessage() {
    Assert.assertNotNull("msg", null);
  }

  @SuppressWarnings("JUnitAssertSameCheck")
  @Test
  public void testAssertSame() {
    Assert.assertSame(obj1, obj1);
    Assert.assertSame("should_never_be_in_the_log", obj1, obj1);
  }

  @Test(expected = AssertionError.class)
  public void testAssertSameFail() {
    Assert.assertSame(obj1, obj1Equal);
  }

  @Test(expected = AssertionError.class)
  public void testAssertSameFailWithMessage() {
    Assert.assertSame("msg", obj1, obj1Equal);
  }

  @Test
  public void testAssertNotSame() {
    Assert.assertNotSame(obj1, obj2);
    Assert.assertNotSame("should_never_be_in_the_log", obj1, obj2);
    Assert.assertNotSame(obj1, obj1Equal);
    Assert.assertNotSame("should_never_be_in_the_log", obj1, obj1Equal);
  }

  @Test(expected = AssertionError.class)
  public void testAssertNotSameFail() {
    Assert.assertNotSame(obj1, obj1);
  }

  @Test(expected = AssertionError.class)
  public void testAssertNotSameFailWithMessage() {
    Assert.assertNotSame("msg", obj1, obj1);
  }

  @Test
  public void testAssertTrue() {
    Assert.assertTrue(true);
    Assert.assertTrue("should_never_be_in_the_log", true);
  }

  @Test(expected = AssertionError.class)
  public void testAssertTrueFail() {
    Assert.assertTrue(false);
  }

  @Test(expected = AssertionError.class)
  public void testAssertTrueFailWithMessage() {
    Assert.assertTrue("msg", false);
  }

  @Test
  public void testAssertFalse() {
    Assert.assertFalse(false);
    Assert.assertFalse("should_never_be_in_the_log", false);
  }

  @Test(expected = AssertionError.class)
  public void testAssertFalseFail() {
    Assert.assertFalse(true);
  }

  @Test(expected = AssertionError.class)
  public void testAssertFalseFailWithMessage() {
    Assert.assertFalse("msg", true);
  }

  @Test
  public void testAssertThrows() {
    NullPointerException npe = new NullPointerException();
    Throwable throwable = Assert.assertThrows(Throwable.class, () -> throwIt(npe));
    Assert.assertSame(npe, throwable);
  }

  @Test(expected = AssertionError.class)
  public void testAssertThrowsFail() {
    Assert.assertThrows(Throwable.class, () -> {});
  }

  @Test
  public void testAssertThrowsFailsWithUnexpectedException() {
    NullPointerException npe = new NullPointerException();
    try {
      Assert.assertThrows(IllegalArgumentException.class, () -> throwIt(npe));
    } catch (AssertionError expected) {
      Assert.assertSame(npe, expected.getCause());
    }
  }

  private static void throwIt(Throwable t) throws Throwable {
    throw t;
  }
}
