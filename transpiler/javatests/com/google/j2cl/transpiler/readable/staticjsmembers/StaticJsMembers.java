package com.google.j2cl.transpiler.readable.staticjsmembers;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class StaticJsMembers {

  @JsProperty(name = "field")
  public static int field1;

  @JsProperty public static int field2;

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  public static class Native {
    @JsProperty(namespace = "Math", name = "PI")
    public static int field3;

    @JsProperty(namespace = GLOBAL, name = "top")
    public static int field4;

    @JsProperty(namespace = "foo.Bar", name = "field")
    public static int field5;

    @JsProperty(namespace = GLOBAL, name = "window.top")
    public static int field6;
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "window.Object")
  public static class Extern {}

  @JsMethod(name = "fun")
  public static void f1(int a) {}

  @JsMethod
  public static void f2(int a) {}

  @JsMethod(namespace = "Math", name = "floor")
  public static native void f3(double a);

  @JsMethod(namespace = GLOBAL, name = "isFinite")
  public static native boolean f4(double a);

  @JsMethod(namespace = "foo.Bar", name = "baz")
  public static native boolean f5();

  @JsMethod(namespace = "foo.Baz", name = "baz")
  public static native boolean f6();

  public void test() {
    StaticJsMembers.f1(1);
    f1(1);
    StaticJsMembers.f2(1);
    f2(1);
    StaticJsMembers.f3(1.1);
    f3(1.1);
    StaticJsMembers.f4(1.1);
    f4(1.1);
    StaticJsMembers.f5();
    f5();

    int n = field1;
    n = field2;
    n = Native.field3;
    n = Native.field4;
    n = Native.field5;
    n = Native.field6;

    new Native();
    new Extern();
  }
}
