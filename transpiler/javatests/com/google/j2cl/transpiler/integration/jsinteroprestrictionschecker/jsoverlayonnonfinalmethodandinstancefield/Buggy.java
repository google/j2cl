package com.google.j2cl.transpiler.integration.jsinteroprestrictionschecker.jsoverlayonnonfinalmethodandinstancefield;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public class Buggy {
  @JsOverlay public final int f2 = 2;

  @JsOverlay
  public void m() {}
}
