package com.google.j2cl.transpiler.readable.nestednativetype;

public class Main {

  /**
   * This proves there is no error by showing that no JSCompiler compile error occurred. If we
   * improperly create the qualified names of types nested inside of native types then we will have
   * created a name collision.
   */
  public static void main(String... args) {
    NativeArray nativeArray = new NativeArray();
    NativeArray2 nativeArray2 = new NativeArray2();

    assert (nativeArray.getObject() != nativeArray.getObject());
  }
}
