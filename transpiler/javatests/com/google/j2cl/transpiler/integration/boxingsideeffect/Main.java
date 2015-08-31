package com.google.j2cl.transpiler.integration.boxingsideeffect;

public class Main {
  public static void main(String... args) {
    Main m = new Main();
    m.testIncrementDecrementSideEffects();
    m.testCompoundAssignmentSideEffects();
    m.testNestedIncrements();
  }

  public Boolean booleanField = new Boolean(true);
  public Double doubleField = new Double(1111.1);
  public Integer integerField = new Integer(1111);
  public Long longField = new Long(1111L);

  public int sideEffectCount;

  public Main causeSideEffect() {
    sideEffectCount++;
    return this;
  }

  public Main fluentAssert(Object expectedValue, Object testValue) {
    assert expectedValue.equals(testValue) : "expected " + expectedValue + " but was " + testValue;
    return this;
  }

  /**
   * Test increment and decrement operators on boxing values.
   */
  public void testIncrementDecrementSideEffects() {
    doubleField = new Double(1111.1);
    integerField = new Integer(1111);
    longField = new Long(1111L);
    sideEffectCount = 0;

    assert sideEffectCount == 0;

    fluentAssert(new Double(1111.1), causeSideEffect().doubleField++);
    assert doubleField.equals(new Double(1112.1));
    assert sideEffectCount == 1;
    fluentAssert(new Integer(1111), causeSideEffect().integerField++);
    assert integerField.equals(new Integer(1112));
    assert sideEffectCount == 2;
    fluentAssert(new Long(1111L), causeSideEffect().longField++);
    assert longField.equals(new Long(1112L));
    assert sideEffectCount == 3;

    fluentAssert(new Double(1112.1), causeSideEffect().doubleField--);
    assert doubleField.equals(new Double(1111.1));
    assert sideEffectCount == 4;
    fluentAssert(new Integer(1112), causeSideEffect().integerField--);
    assert integerField.equals(new Integer(1111));
    assert sideEffectCount == 5;
    fluentAssert(new Long(1112L), causeSideEffect().longField--);
    assert longField.equals(new Long(1111L));
    assert sideEffectCount == 6;

    fluentAssert(new Double(1112.1), ++causeSideEffect().doubleField);
    assert doubleField.doubleValue() == 1112.1;
    assert sideEffectCount == 7;
    fluentAssert(new Integer(1112), ++causeSideEffect().integerField);
    assert integerField.intValue() == 1112;
    assert sideEffectCount == 8;
    fluentAssert(new Long(1112L), ++causeSideEffect().longField);
    assert longField.longValue() == 1112L;
    assert sideEffectCount == 9;

    fluentAssert(new Double(1111.1), --causeSideEffect().doubleField);
    assert doubleField.doubleValue() == 1111.1;
    assert sideEffectCount == 10;
    fluentAssert(new Integer(1111), --causeSideEffect().integerField);
    assert integerField.intValue() == 1111;
    assert sideEffectCount == 11;
    fluentAssert(new Long(1111L), --causeSideEffect().longField);
    assert longField.longValue() == 1111L;
    assert sideEffectCount == 12;
  }

  /**
   * Test compound assignment.
   */
  public void testCompoundAssignmentSideEffects() {
    booleanField = new Boolean(true);
    doubleField = new Double(1111.1);
    integerField = new Integer(1111);
    longField = new Long(1111L);
    sideEffectCount = 0;

    assert sideEffectCount == 0;

    causeSideEffect().doubleField += 10.0;
    assert doubleField.doubleValue() == 1121.1;
    assert sideEffectCount == 1;
    causeSideEffect().integerField += 10;
    assert integerField.intValue() == 1121;
    assert sideEffectCount == 2;
    causeSideEffect().longField += 10L;
    assert longField.longValue() == 1121L;
    assert sideEffectCount == 3;
    causeSideEffect().booleanField &= false;
    assert !booleanField.booleanValue();
  }

  /**
   * Test nested increments.
   */
  public void testNestedIncrements() {
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
