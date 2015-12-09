package com.google.j2cl.transpiler.readable.jsfunction;

import jsinterop.annotations.JsFunction;

/**
 * Notes for restrictions on JsFunction:
 *
 * <p>Direct JsFunction:
 * 1. JsFunction cannot extend other interfaces
 * 2. A type cannot be both a JsFunction and a JsType at the same time.
 * 3. JsFunction cannot have static initializer
 * 4. Cannot be extended by other interfaces.
 *
 * <p>JsFunctionImplementation:
 * 1. Cannot implement more than one interface.
 * 2. Cannot be both a JsFunction implementation and a JsType at the same time.
 * 3. Cannot extend a class.
 * 4. Cannot be subclassed.
 */
@JsFunction
public interface MyJsFunctionInterface {
  int foo(int a);
}
