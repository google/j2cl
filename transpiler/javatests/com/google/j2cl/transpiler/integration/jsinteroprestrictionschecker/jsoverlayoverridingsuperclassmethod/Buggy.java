package com.google.j2cl.transpiler.integration.jsinteroprestrictionschecker.jsoverlayoverridingsuperclassmethod;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public class Buggy extends Super {
  @JsOverlay
  public void m() {}
}
