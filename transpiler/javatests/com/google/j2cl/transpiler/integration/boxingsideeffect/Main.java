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
package com.google.j2cl.transpiler.integration.boxingsideeffect;

import static com.google.j2cl.transpiler.utils.Asserts.assertEquals;
import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

public class Main {
  public static void main(String... args) {
    Main m = new Main();
    m.testIncrementDecrementSideEffects();
    m.testCompoundAssignmentSideEffects();
    m.testNestedIncrements();
  }

  private Boolean booleanField = new Boolean(true);
  private Double doubleField = new Double(1111.1);
  private Integer integerField = new Integer(1111);
  private Long longField = new Long(1111L);

  private int sideEffectCount;

  private Main causeSideEffect() {
    sideEffectCount++;
    return this;
  }

  private Main fluentAssert(Object expectedValue, Object testValue) {
    assertEquals(expectedValue, testValue);
    return this;
  }

  /** Test increment and decrement operators on boxing values. */
  private void testIncrementDecrementSideEffects() {
    doubleField = new Double(1111.1);
    integerField = new Integer(1111);
    longField = new Long(1111L);
    sideEffectCount = 0;

    assertTrue(sideEffectCount == 0);

    fluentAssert(new Double(1111.1), causeSideEffect().doubleField++);
    assertTrue(doubleField.equals(new Double(1112.1)));
    assertTrue(sideEffectCount == 1);
    fluentAssert(new Integer(1111), causeSideEffect().integerField++);
    assertTrue(integerField.equals(new Integer(1112)));
    assertTrue(sideEffectCount == 2);
    fluentAssert(new Long(1111L), causeSideEffect().longField++);
    assertTrue(longField.equals(new Long(1112L)));
    assertTrue(sideEffectCount == 3);

    fluentAssert(new Double(1112.1), causeSideEffect().doubleField--);
    assertTrue(doubleField.equals(new Double(1111.1)));
    assertTrue(sideEffectCount == 4);
    fluentAssert(new Integer(1112), causeSideEffect().integerField--);
    assertTrue(integerField.equals(new Integer(1111)));
    assertTrue(sideEffectCount == 5);
    fluentAssert(new Long(1112L), causeSideEffect().longField--);
    assertTrue(longField.equals(new Long(1111L)));
    assertTrue(sideEffectCount == 6);

    fluentAssert(new Double(1112.1), ++causeSideEffect().doubleField);
    assertTrue(doubleField.doubleValue() == 1112.1);
    assertTrue(sideEffectCount == 7);
    fluentAssert(new Integer(1112), ++causeSideEffect().integerField);
    assertTrue(integerField.intValue() == 1112);
    assertTrue(sideEffectCount == 8);
    fluentAssert(new Long(1112L), ++causeSideEffect().longField);
    assertTrue(longField.longValue() == 1112L);
    assertTrue(sideEffectCount == 9);

    fluentAssert(new Double(1111.1), --causeSideEffect().doubleField);
    assertTrue(doubleField.doubleValue() == 1111.1);
    assertTrue(sideEffectCount == 10);
    fluentAssert(new Integer(1111), --causeSideEffect().integerField);
    assertTrue(integerField.intValue() == 1111);
    assertTrue(sideEffectCount == 11);
    fluentAssert(new Long(1111L), --causeSideEffect().longField);
    assertTrue(longField.longValue() == 1111L);
    assertTrue(sideEffectCount == 12);
  }

  /** Test compound assignment. */
  private void testCompoundAssignmentSideEffects() {
    booleanField = new Boolean(true);
    doubleField = new Double(1111.1);
    integerField = new Integer(1111);
    longField = new Long(1111L);
    sideEffectCount = 0;

    assertTrue(sideEffectCount == 0);

    causeSideEffect().doubleField += 10.0;
    assertTrue(doubleField.doubleValue() == 1121.1);
    assertTrue(sideEffectCount == 1);
    causeSideEffect().integerField += 10;
    assertTrue(integerField.intValue() == 1121);
    assertTrue(sideEffectCount == 2);
    causeSideEffect().longField += 10L;
    assertTrue(longField.longValue() == 1121L);
    assertTrue(sideEffectCount == 3);
    causeSideEffect().booleanField &= false;
    assertTrue(!booleanField.booleanValue());
  }

  /** Test nested increments. */
  private void testNestedIncrements() {
    doubleField = new Double(1111.1);
    integerField = new Integer(1111);
    longField = new Long(1111L);
    sideEffectCount = 0;

    fluentAssert(
        new Long(1113L),
        fluentAssert(new Long(1112L), fluentAssert(new Long(1111L), longField++).longField++)
            .longField++);

    fluentAssert(new Integer(1111), integerField++)
        .fluentAssert(new Integer(1112), integerField++)
        .fluentAssert(new Integer(1113), integerField++);

    fluentAssert(
            new Double(1113.1),
            fluentAssert(new Double(1111.1), doubleField++)
                .fluentAssert(new Double(1112.1), doubleField++)
                .doubleField++)
        .fluentAssert(
            new Double(1116.1),
            fluentAssert(new Double(1114.1), doubleField++)
                .fluentAssert(new Double(1115.1), doubleField++)
                .doubleField++);
  }
}
