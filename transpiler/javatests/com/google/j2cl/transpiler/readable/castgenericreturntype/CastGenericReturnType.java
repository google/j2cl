package com.google.j2cl.transpiler.readable.castgenericreturntype;

/**
 * Tests that a cast is inserted when a method call returns a generic type.
 */
public class CastGenericReturnType<T> {

  public static <T> CastGenericReturnType<T> inferGeneric(T foo) {
    return new CastGenericReturnType<>();
  }

  public static CastGenericReturnType<CastGenericReturnType<String>> tightenType(
      CastGenericReturnType<String> foo) {
    if (foo != null) {
      // Without a cast to fix it, JSCompiler will infer the type of this return statement to be
      // ?Foo<!Foo<?string>>, which does not match the return type, ?Foo<?Foo<?string>>.
      return inferGeneric(foo);
    }
    return null;
  }
}
