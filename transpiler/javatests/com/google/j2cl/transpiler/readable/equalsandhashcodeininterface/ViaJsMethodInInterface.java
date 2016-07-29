package com.google.j2cl.transpiler.readable.equalsandhashcodeininterface;

import jsinterop.annotations.JsMethod;

public interface ViaJsMethodInInterface {

  @JsMethod(name = "equals")
  void notEquals();

  @JsMethod(name = "hashCode")
  void notHashCode();
}
