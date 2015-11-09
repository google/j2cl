package com.google.j2cl.transpiler.integration.strictequality;

public class Main {
  public static void main(String... args) {
    testEqualityIsStrict();
    testBoxedAndDevirtualizedTypes();
  }

  private static void testBoxedAndDevirtualizedTypes() {
    assert new Character((char) 1) != new Character((char) 1);
    assert Character.valueOf((char) 1) == Character.valueOf((char) 1);

    assert new Byte((byte) 1) != new Byte((byte) 1);
    assert Byte.valueOf((byte) 1) == Byte.valueOf((byte) 1);

    assert new Integer(1) != new Integer(1);
    assert Integer.valueOf(1) == Integer.valueOf(1);

    // assert new String("asdf") != new String("asdf"); // can't honor, it's native JS string
    assert String.valueOf("asdf") == String.valueOf("asdf");

    // assert new Boolean(true) != new Boolean(true); // can't honor, it's native JS boolean
    assert Boolean.valueOf(true) == Boolean.valueOf(true);

    // assert new Double(1) != new Double(1); // can't honor, it's native JS double
    assert Double.valueOf(1) == Double.valueOf(1); // different from Java, it's native JS double
  }

  private static void testEqualityIsStrict() {
    Object nullObject = null;
    Object emptyString = "";
    Object boxedBooleanFalse = false;
    Object boxedDoubleZero = 0.0d;
    Object emptyArray = new Object[] {};
    Object undefined = (new Object[1])[0];

    assert undefined == null;
    assert undefined == nullObject;
    assert undefined != boxedBooleanFalse;
    assert undefined != emptyString;
    assert undefined != boxedDoubleZero;
    assert undefined != emptyArray;

    assert null == nullObject;
    assert null != boxedBooleanFalse;
    assert null != emptyString;
    assert null != boxedDoubleZero;
    assert null != emptyArray;

    assert boxedBooleanFalse != nullObject;
    assert boxedBooleanFalse != emptyString;
    assert boxedBooleanFalse != boxedDoubleZero;
    assert boxedBooleanFalse != emptyArray;

    assert boxedDoubleZero != nullObject;
    assert boxedDoubleZero != emptyString;
    assert boxedDoubleZero != emptyArray;

    assert emptyArray != nullObject;
    assert emptyArray != emptyString;

    assert emptyString != nullObject;
  }
}
