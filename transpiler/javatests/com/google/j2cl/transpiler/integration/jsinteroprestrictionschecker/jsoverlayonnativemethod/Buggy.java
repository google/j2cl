package com.google.j2cl.transpiler.integration.jsinteroprestrictionschecker.jsoverlayonnativemethod;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public class Buggy {
  @JsOverlay
  public static final native void m1();

  @JsOverlay
  public final native void m2();
}
