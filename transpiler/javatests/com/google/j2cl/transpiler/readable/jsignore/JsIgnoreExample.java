package com.google.j2cl.transpiler.readable.jsignore;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType
public class JsIgnoreExample {

  public static void exportedFunction() {}

  @JsIgnore
  public static void notExportedFunction() {}

  public static int exportedField = 10;

  @JsIgnore
  public static int notExportedField = 20;

  public static final Object CONSTNAME = new Object();
}
