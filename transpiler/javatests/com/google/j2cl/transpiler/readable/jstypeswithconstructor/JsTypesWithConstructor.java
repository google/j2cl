package com.google.j2cl.transpiler.readable.jstypeswithconstructor;

import jsinterop.annotations.JsType;

/**
 * Currently this class is used to test that no bridge method is created for constructor.
 *
 * <p>The output may be changed when we generate exported JsConstructor properly.
 */
@JsType
public class JsTypesWithConstructor {
  public JsTypesWithConstructor() {}
}
