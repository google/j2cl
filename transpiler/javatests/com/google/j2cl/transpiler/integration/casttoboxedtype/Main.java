package com.google.j2cl.transpiler.integration.casttoboxedtype;

public class Main {
  @SuppressWarnings("unused")
  public static void castToByteException(Object o) {
    try {
      Byte b = (Byte) o;
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  @SuppressWarnings("unused")
  public static void castToDoubleException(Object o) {
    try {
      Double d = (Double) o;
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  @SuppressWarnings("unused")
  public static void castToFloatException(Object o) {
    try {
      Float f = (Float) o;
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  @SuppressWarnings("unused")
  public static void castToIntegerException(Object o) {
    try {
      Integer i = (Integer) o;
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  @SuppressWarnings("unused")
  public static void castToLongException(Object o) {
    try {
      Long l = (Long) o;
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  @SuppressWarnings("unused")
  public static void castToShortException(Object o) {
    try {
      Short s = (Short) o;
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  @SuppressWarnings("unused")
  public static void castToCharacterException(Object o) {
    try {
      Character c = (Character) o;
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  @SuppressWarnings("unused")
  public static void castToBooleanException(Object o) {
    try {
      Boolean s = (Boolean) o;
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  @SuppressWarnings("unused")
  public static void castToNumberException(Object o) {
    try {
      Number n = (Number) o;
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  @SuppressWarnings("unused")
  public static void main(String[] args) {
    Object b = new Byte((byte) 1);
    Byte bb = (Byte) b;
    Number n = (Number) b;
    castToDoubleException(b);
    castToFloatException(b);
    castToIntegerException(b);
    castToLongException(b);
    castToShortException(b);
    castToCharacterException(b);
    castToBooleanException(b);

    Object d = new Double(1.0);
    Double dd = (Double) d;
    n = (Number) d;
    castToByteException(d);
    castToFloatException(d);
    castToIntegerException(d);
    castToLongException(d);
    castToShortException(d);
    castToCharacterException(d);
    castToBooleanException(d);

    Object f = new Float(1.0f);
    Float ff = (Float) f;
    n = (Number) f;
    castToByteException(f);
    castToDoubleException(f);
    castToIntegerException(f);
    castToLongException(f);
    castToShortException(f);
    castToCharacterException(f);
    castToBooleanException(f);

    Object i = new Integer(1);
    Integer ii = (Integer) i;
    n = (Number) i;
    castToByteException(i);
    castToDoubleException(i);
    castToFloatException(i);
    castToLongException(i);
    castToShortException(i);
    castToCharacterException(i);
    castToBooleanException(i);

    Object l = new Long(1L);
    Long ll = (Long) l;
    n = (Number) l;
    castToByteException(l);
    castToDoubleException(l);
    castToFloatException(l);
    castToIntegerException(l);
    castToShortException(l);
    castToCharacterException(l);
    castToBooleanException(l);

    Object s = new Short((short) 1);
    Short ss = (Short) s;
    n = (Number) s;
    castToByteException(s);
    castToDoubleException(s);
    castToFloatException(s);
    castToIntegerException(s);
    castToLongException(s);
    castToCharacterException(s);
    castToBooleanException(s);

    Object c = new Character('a');
    Character cc = (Character) c;
    castToByteException(c);
    castToDoubleException(c);
    castToFloatException(c);
    castToIntegerException(c);
    castToLongException(c);
    castToShortException(c);
    castToNumberException(c);
    castToBooleanException(c);

    Object bool = new Boolean(true);
    Boolean bbool = (Boolean) bool;
    castToByteException(bool);
    castToDoubleException(bool);
    castToFloatException(bool);
    castToIntegerException(bool);
    castToLongException(bool);
    castToShortException(bool);
    castToNumberException(bool);
    castToCharacterException(bool);

    Object sn = new SubNumber();
    n = (Number) sn;
  }
}
