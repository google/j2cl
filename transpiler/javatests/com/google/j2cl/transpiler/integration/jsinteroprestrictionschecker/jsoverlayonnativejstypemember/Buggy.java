package com.google.j2cl.transpiler.integration.jsinteroprestrictionschecker.jsoverlayonnativejstypemember;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public class Buggy {
  @JsOverlay public static Object object = new Object();

  @JsOverlay
  public static void m() {}

  @JsOverlay
  public static void m(int x) {}

  @JsOverlay
  private static void m(boolean x) {}

  @JsOverlay
  public final void n() {}

  @JsOverlay
  public final void n(int x) {}

  @JsOverlay
  private final void n(boolean x) {}

  @JsOverlay
  final void o() {}

  @JsOverlay
  protected final void p() {}
}
