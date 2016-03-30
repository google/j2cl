package com.google.j2cl.transpiler.readable.objectmap;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
public class ObjectMap<T> {
  @JsOverlay
  static <T> ObjectMap<T> create() {
    return null;
  }

  @JsOverlay
  static ObjectMap<String> createForString() {
    return null;
  }
}
