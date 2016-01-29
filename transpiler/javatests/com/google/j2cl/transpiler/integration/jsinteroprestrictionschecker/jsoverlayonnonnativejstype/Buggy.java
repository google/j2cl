package com.google.j2cl.transpiler.integration.jsinteroprestrictionschecker.jsoverlayonnonnativejstype;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType
public class Buggy {
  @JsOverlay public static final int F = 2;

  @JsOverlay
  public final void m() {};
}
