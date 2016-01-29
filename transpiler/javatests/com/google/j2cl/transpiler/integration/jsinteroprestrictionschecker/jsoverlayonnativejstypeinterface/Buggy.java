package com.google.j2cl.transpiler.integration.jsinteroprestrictionschecker.jsoverlayonnativejstypeinterface;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface Buggy {
  @JsOverlay Object obj = new Object();
  // TODO: uncomment once default method is supported.
  // @JsOverlay default void someOverlayMethod(){};
}
