package a;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public class A {
  @JsOverlay
  public final String overlayMethod() {
    return "don't prune me please!";
  }
}
