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
package com.google.j2cl.jre.java.util;

import static com.google.j2cl.jre.testing.TestUtils.isWasm;
import static org.junit.Assert.assertThrows;

import java.util.StringJoiner;
import junit.framework.TestCase;

/** Tests StringJoiner. */
public class StringJoinerTest extends TestCase {

  private StringJoiner joiner;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    joiner = new StringJoiner("|", "[", "]");
  }

  @SuppressWarnings("AssertThrowsMultipleStatements")
  public void testConstructor_null() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    assertThrows(
        NullPointerException.class,
        () -> {
          var joiner = new StringJoiner(null, null, null);
          fail(joiner.toString()); // keep the instance to avoid removal of the constructor call.
        });
  }

  public void testAdd() throws Exception {
    joiner.add("0").add(null);
    assertEquals("[0|null]", joiner.toString());
  }

  public void testLength() throws Exception {
    assertEquals(joiner.toString().length(), joiner.length());

    joiner.setEmptyValue("empty");
    assertEquals(joiner.toString().length(), joiner.length());

    joiner.add("0").add("1");
    assertEquals(joiner.toString().length(), joiner.length());
  }

  public void testMerge() throws Exception {
    joiner.add("0").add("1");
    joiner.merge(new StringJoiner(",", "(", ")").add("2").add("3"));
    joiner.add("4").add("5");
    assertEquals("[0|1|2,3|4|5]", joiner.toString());

    joiner.merge(joiner);
    assertEquals("[0|1|2,3|4|5|0|1|2,3|4|5]", joiner.toString());
  }

  public void testMerge_null() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    assertThrows(NullPointerException.class, () -> joiner.merge(null));
  }

  public void testSetEmptyValue() throws Exception {
    joiner.setEmptyValue("empty");
    assertEquals("empty", joiner.toString());
  }

  public void testSetEmptyValue_null() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    assertThrows(NullPointerException.class, () -> joiner.setEmptyValue(null));
  }
}
