package com.google.j2cl.transpiler.readable.aptgeneratordep;

public class Foo {
  // Use an annotation, to trigger the APT DummyProcessor.
  @Override
  public String toString() {
    // This can only compile if the APT synthesizes the "Dummy" class.
    return Dummy.class.getSimpleName();
  }
}
