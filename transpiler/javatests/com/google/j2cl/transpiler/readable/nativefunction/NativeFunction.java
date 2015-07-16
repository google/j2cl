package com.google.j2cl.transpiler.readable.nativefunction;

public class NativeFunction {
  public static native void doFoo();

  public native void doBar();

  public void main() {
    NativeFunction.doFoo();
    this.doBar();
  }
}
