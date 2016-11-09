package com.google.j2cl.transpiler.integration.localgenericclassinitjustonce;

/**
 * The 'value' field is given a value of 0 in it's initializer, which should run in the $init()
 * function. This value is then replaced with another value in the body of the second P1 constructor
 * method. It's important that constructors that delegate to 'this()' not also call $init() since
 * doing so would mean it is called more than once and values that were set in other constructors
 * (invoked by the 'this()' call) will have been overwritten.
 */
public class Main {

  static class P1<T1> {

    class P2<T2> extends P1<T1> {

      int value = 0;

      P2() {
        this(1);
      }

      P2(int i) {
        value = i;
      }
    }
  }

  public static void main(String[] args) {
    P1<?> p1 = new P1<Object>();
    P1<?>.P2<?> p2 = p1.new P2<Object>();
    assert (1 == p2.value);
    assert (2 == p1.new P2<Object>(2).value);
  }
}
