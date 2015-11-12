package com.google.j2cl.transpiler.readable.unimplementedbridgemethod;

interface I<T> {
  int foo(T t);
}

interface J extends I<String> {}

/**
 * No bridge method is needed.
 * 
 * <p>A synthesized abstract method m_foo__java_lang_Object() is generated to implement J.foo()
 * to avoid JSCompiler warning.
 */
abstract class Bar implements J {}

/**
 * No bridge method is needed.
 * 
 * <p>A synthesized abstract method m_foo__java_lang_Object() is generated to implement
 * I.foo(T) to avoid JSCompiler warning.
 */
public abstract class Foo implements I<String> {
}
