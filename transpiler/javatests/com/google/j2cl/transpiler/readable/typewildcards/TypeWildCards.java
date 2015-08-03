package com.google.j2cl.transpiler.readable.typewildcards;

class GenericType<T> {}

public class TypeWildCards {
  public void unbounded(GenericType<?> g) {}

  public void upperBound(GenericType<? extends TypeWildCards> g) {}

  public void lowerBound(GenericType<? super TypeWildCards> g) {}

  public void test() {
    unbounded(new GenericType<TypeWildCards>());
    upperBound(new GenericType<TypeWildCards>());
    lowerBound(new GenericType<TypeWildCards>());
  }
}
