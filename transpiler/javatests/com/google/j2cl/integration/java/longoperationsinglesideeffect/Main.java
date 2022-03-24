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
package longoperationsinglesideeffect;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void main(String... args) {
    Main m = new Main();
    m.testIncrementDecrementSideEffects();
    m.testCompoundAssignmentSideEffects();
    m.testNestedIncrements();
  }

  public long longField = 0L;

  public int sideEffectCount;

  public Main causeSideEffect() {
    sideEffectCount++;
    return this;
  }

  public Main fluentAssert(long expectedValue, long testValue) {
    assertTrue("expected " + expectedValue + " but was " + testValue, expectedValue == testValue);
    return this;
  }

  /**
   * Long emulation of increment and decrement operators forces multiple accesses of the subject
   * field but it's important that it be done in a way that doesn't invoke earlier parts of the
   * qualifier chain multiple times.
   */
  public void testIncrementDecrementSideEffects() {
    longField = 0L;
    sideEffectCount = 0;

    assertTrue(sideEffectCount == 0);

    assertTrue(causeSideEffect().longField++ == 0L);
    assertTrue(longField == 1L);
    assertTrue(sideEffectCount == 1);

    assertTrue(causeSideEffect().longField-- == 1L);
    assertTrue(longField == 0L);
    assertTrue(sideEffectCount == 2);

    assertTrue(++causeSideEffect().longField == 1L);
    assertTrue(longField == 1L);
    assertTrue(sideEffectCount == 3);

    assertTrue(--causeSideEffect().longField == 0L);
    assertTrue(longField == 0L);
    assertTrue(sideEffectCount == 4);

    causeSideEffect().longField += 1L;
    assertTrue(longField == 1L);
    assertTrue(sideEffectCount == 5);

    causeSideEffect().longField -= 1L;
    assertTrue(longField == 0L);
    assertTrue(sideEffectCount == 6);
  }

  /**
   * Long emulation of increment and decrement operators forces multiple accesses of the subject
   * field but it's important that it be done in a way that doesn't invoke earlier parts of the
   * qualifier chain multiple times.
   */
  public void testCompoundAssignmentSideEffects() {
    longField = 0L;
    sideEffectCount = 0;

    assertTrue(sideEffectCount == 0);

    causeSideEffect().longField += 1L;
    assertTrue(longField == 1L);
    assertTrue(sideEffectCount == 1);

    causeSideEffect().longField -= 1L;
    assertTrue(longField == 0L);
    assertTrue(sideEffectCount == 2);

    causeSideEffect().longField *= 1L;
    assertTrue(longField == 0L);
    assertTrue(sideEffectCount == 3);

    causeSideEffect().longField /= 1L;
    assertTrue(longField == 0L);
    assertTrue(sideEffectCount == 4);
  }

  /**
   * Stresses the Long emulation increment rewriting.
   */
  public void testNestedIncrements() {
    longField = 0L;
    sideEffectCount = 0;

    fluentAssert(2L, fluentAssert(1L, fluentAssert(0L, longField++).longField++).longField++);

    longField = 0L;
    fluentAssert(0L, longField++).fluentAssert(1L, longField++).fluentAssert(2L, longField++);

    longField = 0L;
    fluentAssert(2L, fluentAssert(0L, longField++).fluentAssert(1L, longField++).longField++)
        .fluentAssert(5L, fluentAssert(3L, longField++).fluentAssert(4L, longField++).longField++);
  }
}
