package com.google.j2cl.transpiler.integration.jsconstructornested;

import jsinterop.annotations.JsType;

public class Main {

  String name = "value one";
  InnerJsType innerJsType = new InnerJsType();

  /** The inner JsType in question. */
  @JsType(namespace = "foo", name = "Bar")
  public class InnerJsType {
    public String getOuterName() {
      return name;
    }

    public void setOuterName(String name) {
      Main.this.name = name;
    }
  }

  /**
   * A JsInterop calling interface against InnerJsType. We use this to prove that InnerJsType's raw
   * JS constructor does receive an $outer_this parameter.
   */
  @JsType(isNative = true, namespace = "foo", name = "Bar")
  static class InnerJsTypeFacade {
    InnerJsTypeFacade(Main outerThis) {}

    native String getOuterName();

    native String setOuterName(String name);
  }

  /**
   * Test seeks to prove *both* that inner classes annotated with @JsType are getting @JsConstructor
   * implicitly added to their constructor *and* that constructor receives and properly uses an
   * $outer_this parameter.
   */
  public static void main(String... args) {
    Main main = new Main();

    // Exercises the class via Java. Prove that the $outer_this is arriving and being used
    // properly but doesn't prove whether it is arriving via the raw JS constructor or if it's
    // actually arriving via a $ctor.
    {
      InnerJsType innerJsType = main.innerJsType;
      assert "value one".equals(innerJsType.getOuterName());
      innerJsType.setOuterName("value two");
      assert "value two".equals(innerJsType.getOuterName());
    }

    // Exercises the class via JsInterop. Proves that the $outer_this is definitely arriving via the
    // raw JS constructor.
    {
      InnerJsTypeFacade innerJsTypeFacade = new InnerJsTypeFacade(main);
      assert "value two".equals(innerJsTypeFacade.getOuterName());
      innerJsTypeFacade.setOuterName("value three");
      assert "value three".equals(innerJsTypeFacade.getOuterName());
    }
  }
}
