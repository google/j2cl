package com.google.j2cl.transpiler.readable.bridgemethodsreturn;

public class B extends A<Number, String> implements I<Integer, String> {
  // bridge method for A.fun(String):Number and I.fun(String):Integer should both delegate
  // to this method. Since A.fun(String):Number and I.fun(String):Integer has the same signature
  // (although) different return type, only one bridge method should be created.
  public Integer fun(String s) {
    return new Integer(1);
  }
}
