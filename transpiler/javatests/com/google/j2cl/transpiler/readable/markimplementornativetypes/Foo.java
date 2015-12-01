package com.google.j2cl.transpiler.readable.markimplementornativetypes;

import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "test.foo")
interface MyNativeInterface {}

interface RegularInterface {}

interface SubNativeInterface extends MyNativeInterface, RegularInterface {}

public class Foo implements MyNativeInterface, RegularInterface {}
