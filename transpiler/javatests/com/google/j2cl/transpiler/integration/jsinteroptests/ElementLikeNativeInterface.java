package com.google.j2cl.transpiler.integration.jsinteroptests;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * An interface that represent part of HTML element's contract.
 */
@JsType(isNative = true, namespace = "test.foo")
public interface ElementLikeNativeInterface {
  @JsProperty
  String getTagName();
}
