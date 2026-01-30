/*
 * Copyright 2008 Google Inc.
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
package com.google.j2cl.jre.java.sql;

import static org.junit.Assert.assertThrows;

import com.google.j2cl.jre.testing.J2ktIncompatible;
import java.sql.Time;
import junit.framework.TestCase;

/**
 * Tests {@link java.sql.Time}. We assume that the underlying {@link java.util.Date} implementation
 * is correct and concentrate only on the differences between the two.
 */
@SuppressWarnings("deprecation")
public class SqlTimeTest extends TestCase {

  @SuppressWarnings("DoNotCall")
  public void testUnimplementedFunctions() {
    Time d = new Time(0);

    assertThrows(IllegalArgumentException.class, () -> d.getYear());

    assertThrows(IllegalArgumentException.class, () -> d.getMonth());

    assertThrows(IllegalArgumentException.class, () -> d.getDate());

    assertThrows(IllegalArgumentException.class, () -> d.getDay());

    assertThrows(IllegalArgumentException.class, () -> d.setYear(0));

    assertThrows(IllegalArgumentException.class, () -> d.setMonth(0));

    assertThrows(IllegalArgumentException.class, () -> d.setDate(0));
  }

  @J2ktIncompatible // Not nullable according to Jspecify
  public void testParseNull() {
    assertThrows(IllegalArgumentException.class, () -> Time.parse(null));
  }

  public void testParse() {
    try {
      Time.parse("");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    Time t = Time.valueOf("13:01:30");
    assertEquals(13, t.getHours());
    assertEquals(1, t.getMinutes());
    assertEquals(30, t.getSeconds());

    Time d2 = Time.valueOf(t.toString());
    assertEquals(t, d2);

    // tests to see if the various parts are indeed decoded in base-10 (till
    // r3728 the base was first inferred)
    Time t2 = Time.valueOf("08:09:01");
    assertEquals(8, t2.getHours());
    assertEquals(9, t2.getMinutes());
    assertEquals(1, t2.getSeconds());
    assertEquals(t2, Time.valueOf(t2.toString()));
  }

  public void testToString() {
    Time time = new Time(12, 34, 56);
    assertEquals("12:34:56", time.toString());
  }

  public void testInternalPrecision() {
    long millis = 1283895273475L;
    Time today = new Time(millis);
    Time after = new Time(today.getTime() + 1);
    Time before = new Time(today.getTime() - 1);

    // Note that Times internally retain millisecond precision
    assertTrue(after.after(today));
    assertTrue(before.before(today));
  }
}
