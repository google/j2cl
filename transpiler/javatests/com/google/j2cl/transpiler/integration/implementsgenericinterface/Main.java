package com.google.j2cl.transpiler.integration.implementsgenericinterface;

interface GenericInterface<T> {
  T foo(T t);
}

class InterfaceImpl implements GenericInterface<InterfaceImpl> {
  @Override
  public InterfaceImpl foo(InterfaceImpl t) {
    return t;
  }
}

class InterfaceGenericImpl<T> implements GenericInterface<T> {
  @Override
  public T foo(T t) {
    return t;
  }
}

public class Main {
  public static void main(String[] args) {
    InterfaceImpl i = new InterfaceImpl();
    Object o = i.foo(i);
    assert o == i;

    InterfaceGenericImpl<InterfaceImpl> gi = new InterfaceGenericImpl<>();
    Object oo = gi.foo(i);
    assert oo == i;
  }
}
