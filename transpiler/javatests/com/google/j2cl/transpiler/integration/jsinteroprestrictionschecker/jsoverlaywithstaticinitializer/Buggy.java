package com.google.j2cl.transpiler.integration.jsinteroprestrictionschecker.jsoverlaywithstaticinitializer;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public class Buggy {
  @JsOverlay public static final Object f1 = new Object();
  @JsOverlay public static int f2 = 2;
}
