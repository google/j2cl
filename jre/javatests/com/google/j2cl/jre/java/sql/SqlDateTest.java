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

import java.sql.Date;
import junit.framework.TestCase;

/**
 * Tests {@link java.sql.Date}. We assume that the underlying {@link java.util.Date} implementation
 * is correct and concentrate only on the differences between the two.
 */
@SuppressWarnings({"deprecation", "DoNotCall"})
public class SqlDateTest extends TestCase {

  public void testInternalPrecision() {
    long millis = 1283895273475L;
    Date now = new Date(millis);
    Date after = new Date(now.getTime() + 1);
    Date before = new Date(now.getTime() - 1);

    // Note that Dates internally retain millisecond precision
    assertTrue(after.after(now));
    assertTrue(before.before(now));
  }

  public void testRoundedToDay() {
    java.util.Date utilDate = new java.util.Date();
    Date sqlDate = new Date(utilDate.getTime());

    java.util.Date utilDate2 = new java.util.Date(sqlDate.getTime());

    assertEquals(utilDate.getYear(), utilDate2.getYear());
    assertEquals(utilDate.getMonth(), utilDate2.getMonth());
    assertEquals(utilDate.getDate(), utilDate2.getDate());
  }

  public void testToString() {
    Date sqlDate = new Date(2000 - 1900, 1 - 1, 1);
    assertEquals("2000-01-01", sqlDate.toString());
  }

  public void testUnimplementedFunctions() {
    Date d = new Date(0);

    assertThrows(IllegalArgumentException.class, () -> d.getHours());

    assertThrows(IllegalArgumentException.class, () -> d.getMinutes());

    assertThrows(IllegalArgumentException.class, () -> d.getSeconds());

    assertThrows(IllegalArgumentException.class, () -> d.setHours(0));

    assertThrows(IllegalArgumentException.class, () -> d.setMinutes(0));

    assertThrows(IllegalArgumentException.class, () -> d.setSeconds(0));
  }

  public void testValueOf() {
    Date d = Date.valueOf("2008-03-26");
    // Months are 0-based, days are 1-based
    assertEquals(108, d.getYear());
    assertEquals(2, d.getMonth());
    assertEquals(26, d.getDate());

    Date d2 = Date.valueOf(d.toString());
    assertEquals(d, d2);

    // validate that leading zero's don't trigger octal eval
    d = Date.valueOf("2009-08-08");
    assertEquals(109, d.getYear());
    assertEquals(7, d.getMonth());
    assertEquals(8, d.getDate());

    // validate 0x isn't a valid prefix
    assertThrows(IllegalArgumentException.class, () -> Date.valueOf("2009-0xA-0xB"));
  }
}
