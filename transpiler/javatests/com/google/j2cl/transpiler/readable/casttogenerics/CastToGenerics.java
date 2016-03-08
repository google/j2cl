package com.google.j2cl.transpiler.readable.casttogenerics;

public class CastToGenerics<T, E extends Number> {
  @SuppressWarnings({"unused", "unchecked", "rawtypes"})
  public void test() {
    Object o = new Integer(1);
    E e = (E) o; // cast to type varaible with bound
    T t = (T) o; // cast to type varaible without bound
    E[] es = (E[]) o; // cast to array of type varaible.
    T[] ts = (T[]) o;

    Object c = new CastToGenerics<String, Number>();
    // cast to parameterized type.
    CastToGenerics<Error, Number> cc = (CastToGenerics<Error, Number>) c;
    cc = (CastToGenerics) c;
  }

  @SuppressWarnings({"unchecked", "unused", "cast"})
  public <S, V extends Enum<V>> void castToTypeVariable() {
    Object o = new Integer(1);
    S s = (S) o; // cast to type variable declared by the method.
    Object c = (CastToGenerics<S, Number>) o;
    c = (S[]) o;
    c = (V) o;
  }

  public static <T> Foo<T> doSomething() {
    return new Foo<T>() { };
  }

  public static class Foo<V> { }
}
