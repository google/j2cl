package com.google.j2cl.transpiler.readable.transitivejsoverlayimport;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
class Transitive {

  @JsProperty
  final native int getJsProperty();

  @JsOverlay
  final void doTransitiveInstanceMethod() {}
}
