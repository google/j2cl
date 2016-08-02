package com.google.j2cl.transpiler.readable.defaultmethodstwointerfaces;

interface InterfaceWithDefaultMethod extends PureInterface<String> {

  @Override
  default void run(String string) {}
}
