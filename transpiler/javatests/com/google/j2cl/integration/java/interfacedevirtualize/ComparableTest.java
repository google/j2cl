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
package interfacedevirtualize;

import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test Comparable Interface on all devirtualized classes that implement it.
 */
public class ComparableTest {
  public <T> int compare0(Comparable<T> c, T t) {
    return c.compareTo(t);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public int compare1(Comparable c, Object o) {
    return c.compareTo(o);
  }

  public int compare2(Comparable<Double> c, Double d) {
    return c.compareTo(d);
  }

  public int compare2(Comparable<Boolean> c, Boolean b) {
    return c.compareTo(b);
  }

  public int compare2(Comparable<Integer> c, Integer i) {
    return c.compareTo(i);
  }

  public int compare2(Comparable<Long> c, Long l) {
    return c.compareTo(l);
  }

  public int compare2(Comparable<String> s, String si) {
    return s.compareTo(si);
  }

  public int compare2(Comparable<ComparableImpl> c, ComparableImpl ci) {
    return c.compareTo(ci);
  }

  public int compare3(Double d1, Double d2) {
    return d1.compareTo(d2);
  }

  public int compare3(Boolean b1, Boolean b2) {
    return b1.compareTo(b2);
  }

  public int compare3(Integer i1, Integer i2) {
    return i1.compareTo(i2);
  }

  public int compare3(Long l1, Long l2) {
    return l1.compareTo(l2);
  }

  public int compare3(String s1, String s2) {
    return s1.compareTo(s2);
  }

  public int compare3(ComparableImpl c1, ComparableImpl c2) {
    return c1.compareTo(c2);
  }

  public void testDouble() {
    Double d1 = new Double(1.1);
    Double d2 = new Double(1.1);
    Double d3 = new Double(2.1);
    assertTrue(compare0(d1, d2) == 0);
    assertTrue(compare0(d1, d3) < 0);
    assertTrue(compare0(d3, d2) > 0);

    assertTrue(compare1(d1, d2) == 0);
    assertTrue(compare1(d1, d3) < 0);
    assertTrue(compare1(d3, d2) > 0);

    assertTrue(compare2(d1, d2) == 0);
    assertTrue(compare2(d1, d3) < 0);
    assertTrue(compare2(d3, d2) > 0);

    assertTrue(compare3(d1, d2) == 0);
    assertTrue(compare3(d1, d3) < 0);
    assertTrue(compare3(d3, d2) > 0);

    assertThrowsClassCastException(() -> compare1(d1, new Object()));
  }

  public void testBoolean() {
    Boolean b1 = new Boolean(false);
    Boolean b2 = new Boolean(false);
    Boolean b3 = new Boolean(true);
    assertTrue(compare0(b1, b2) == 0);
    assertTrue(compare0(b1, b3) < 0);
    assertTrue(compare0(b3, b2) > 0);

    assertTrue(compare1(b1, b2) == 0);
    assertTrue(compare1(b1, b3) < 0);
    assertTrue(compare1(b3, b2) > 0);

    assertTrue(compare2(b1, b2) == 0);
    assertTrue(compare2(b1, b3) < 0);
    assertTrue(compare2(b3, b2) > 0);

    assertTrue(compare3(b1, b2) == 0);
    assertTrue(compare3(b1, b3) < 0);
    assertTrue(compare3(b3, b2) > 0);

    assertThrowsClassCastException(() -> compare1(b1, new Object()));
  }

  public void testInteger() {
    Integer i1 = new Integer(1000);
    Integer i2 = new Integer(1000);
    Integer i3 = new Integer(2000);
    assertTrue(compare0(i1, i2) == 0);
    assertTrue(compare0(i1, i3) < 0);
    assertTrue(compare0(i3, i2) > 0);

    assertTrue(compare1(i1, i2) == 0);
    assertTrue(compare1(i1, i3) < 0);
    assertTrue(compare1(i3, i2) > 0);

    assertTrue(compare2(i1, i2) == 0);
    assertTrue(compare2(i1, i3) < 0);
    assertTrue(compare2(i3, i2) > 0);

    assertTrue(compare3(i1, i2) == 0);
    assertTrue(compare3(i1, i3) < 0);
    assertTrue(compare3(i3, i2) > 0);

    assertThrowsClassCastException(() -> compare1(i1, new Object()));
  }

  public void testLong() {
    Long l1 = new Long(1000L);
    Long l2 = new Long(1000L);
    Long l3 = new Long(2000L);
    assertTrue(compare0(l1, l2) == 0);
    assertTrue(compare0(l1, l3) < 0);
    assertTrue(compare0(l3, l2) > 0);

    assertTrue(compare1(l1, l2) == 0);
    assertTrue(compare1(l1, l3) < 0);
    assertTrue(compare1(l3, l2) > 0);

    assertTrue(compare2(l1, l2) == 0);
    assertTrue(compare2(l1, l3) < 0);
    assertTrue(compare2(l3, l2) > 0);

    assertTrue(compare3(l1, l2) == 0);
    assertTrue(compare3(l1, l3) < 0);
    assertTrue(compare3(l3, l2) > 0);

    assertThrowsClassCastException(() -> compare1(l1, new Object()));
  }

  public void testString() {
    String s1 = "abc";
    String s2 = "abc";
    String s3 = "def";
    assertTrue(compare0(s1, s2) == 0);
    assertTrue(compare0(s1, s3) < 0);
    assertTrue(compare0(s3, s2) > 0);

    assertTrue(compare1(s1, s2) == 0);
    assertTrue(compare1(s1, s3) < 0);
    assertTrue(compare1(s3, s2) > 0);

    assertTrue(compare2(s1, s2) == 0);
    assertTrue(compare2(s1, s3) < 0);
    assertTrue(compare2(s3, s2) > 0);

    assertTrue(compare3(s1, s2) == 0);
    assertTrue(compare3(s1, s3) < 0);
    assertTrue(compare3(s3, s2) > 0);

    assertThrowsClassCastException(() -> compare1(s1, new Object()));
  }

  public void testComparableImpl() {
    ComparableImpl c1 = new ComparableImpl(1000);
    ComparableImpl c2 = new ComparableImpl(1000);
    ComparableImpl c3 = new ComparableImpl(2000);
    assertTrue(compare0(c1, c2) == 0);
    assertTrue(compare0(c1, c3) < 0);
    assertTrue(compare0(c3, c2) > 0);

    assertTrue(compare1(c1, c2) == 0);
    assertTrue(compare1(c1, c3) < 0);
    assertTrue(compare1(c3, c2) > 0);

    assertTrue(compare2(c1, c2) == 0);
    assertTrue(compare2(c1, c3) < 0);
    assertTrue(compare2(c3, c2) > 0);

    assertTrue(compare3(c1, c2) == 0);
    assertTrue(compare3(c1, c3) < 0);
    assertTrue(compare3(c3, c2) > 0);

    assertThrowsClassCastException(() -> compare1(c1, new Object()));
  }

  public static void test() {
    ComparableTest test = new ComparableTest();
    test.testDouble();
    test.testBoolean();
    test.testInteger();
    test.testLong();
    test.testString();
    test.testComparableImpl();
  }
}

