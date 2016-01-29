package com.google.j2cl.transpiler.integration.jsinteroprestrictionschecker.jsoverlayimplementinginterfacemethod;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public class Buggy implements IBuggy {
  @JsOverlay
  public void m() {}
}
