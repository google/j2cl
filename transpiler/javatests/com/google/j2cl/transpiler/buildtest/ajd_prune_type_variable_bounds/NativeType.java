package n;

import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "a", name = "Native")
public class NativeType {
  public native void doSomething();
}
