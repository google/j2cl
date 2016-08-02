package com.google.j2cl.transpiler.readable.collisions;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class goog {}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
class Blah {
  @JsMethod(namespace = JsPackage.GLOBAL, name = "Math.random")
  static native void m();

  @JsProperty(name = "prototype.length")
  static native double getN();
}

class foo {}

class bar {
  {
    int foo, bar, goog, flip, window;
    Blah.m();
    Blah.getN();
    new Blah();
    new goog();
    new foo();
  }
}
