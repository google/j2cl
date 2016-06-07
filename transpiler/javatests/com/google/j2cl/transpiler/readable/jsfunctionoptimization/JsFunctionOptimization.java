package com.google.j2cl.transpiler.readable.jsfunctionoptimization;

import jsinterop.annotations.JsFunction;

public class JsFunctionOptimization {

  @JsFunction
  interface F {
    String m(String s);
  }

  public void main(final int r) {
    new Object() {
      void m() {
        String x = "";
        new Object() {
          void m() {
            final int var = 1;
            F f =
                new F() {

                  @Override
                  public String m(String s) {
                    final int r1 = r;
                    final int var1 = var;
                    final String x1 = x;
                    return String.valueOf(r)
                        + s
                        + x
                        + var
                        + new F() {
                          @Override
                          public String m(String s) {
                            return s + r1 + x1 + var1;
                          }
                        }.m("hello");
                  }
                };
            F f2 =
                new F() {

                  @Override
                  public String m(String s) {
                    final int r1 = r;
                    final int var1 = var;
                    final String x1 = x;
                    return String.valueOf(r)
                        + s
                        + x
                        + var
                        + new Object() {
                          @Override
                          public String toString() {
                            return "Hey";
                          }
                        }.toString();
                  }
                };
          }
        };
      }
    };
  }
}
