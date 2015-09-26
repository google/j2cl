package com.google.j2cl.transpiler.integration.casttogenerics;

public class Main<T, E extends Number> {
  @SuppressWarnings({"unused", "unchecked"})
  public void test() {
    Object o = new Integer(1);
    E e = (E) o; // cast to type varaible with bound, casting Integer instance to Number
    T t = (T) o; // cast to type varaible without bound, casting Integer instance to Object

    Object oo = new Error();
    try {
      E ee = (E) oo; // casting Error instance to Number, exception.
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException exe) {
      // expected.
    }

    Object c = new Main<Number, Number>();
    Main<Error, Number> cc = (Main<Error, Number>) c; // cast to parameterized type.

    Object[] is = new Integer[1];
    Object[] os = new Object[1];
    E[] es = (E[]) is;
    T[] ts = (T[]) is;
    try {
      E[] ees = (E[]) os;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException exe) {
      // expected.
    }
    T[] tts = (T[]) os;
  }

  public static void main(String... args) {
    Main<Integer, Integer> m = new Main<Integer, Integer>();
    m.test();
  }
}
