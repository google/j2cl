package com.google.j2cl.transpiler.readable.jstypewithdefault;

import jsinterop.annotations.JsType;

public class SomeJsType implements Interface {}

@JsType
interface Interface {
  default void defaultMethod() {}
}
