package com.google.j2cl.transpiler.integration.jsoverlaywithjsfunction;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    assert new Foo().bar() == 30;
  }

  @JsType(isNative = true, namespace = GLOBAL, name = "Object")
  public static class Foo {
    @JsOverlay
    public final int bar() {
      return new Intf() {
        @Override
        public int run(int x, int y) {
          return x + y;
        }
      }.run(10, 20);
    }
  }

  @JsFunction
  private interface Intf {
    int run(int x, int y);
  }
}
