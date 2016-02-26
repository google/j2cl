package com.google.j2cl.transpiler.readable.jsfunctiontests;

public class EqualFunctions {
  private EqualFunctions() {}

  public static EqualFunction<Object> forObject =
      new EqualFunction<Object>() {
        @Override
        public boolean equal(Object first, Object second) {
          return first == second;
        }
      };

  @SuppressWarnings("unchecked")
  public static final <T> EqualFunction<T> objectEqualFunction() {
    return (EqualFunction<T>) forObject;
  }

  @SuppressWarnings("unchecked")
  public static final <T> EqualFunction<T> collectionEqualFunction() {
    return (EqualFunction<T>) forCollection;
  }

  public static final EqualFunction<Integer> forInteger = objectEqualFunction();

  /**
   * Equal function handling array equal logic in addition to object equal.
   */
  public static EqualFunction<Object> forCollection = withCollection(forObject);

  /**
   * Returns a equals function that can handle array equal logic in addition to the custom equal
   * function.
   */
  public static <T> EqualFunction<T> withCollection(final EqualFunction<T> customFunction) {
    return new EqualFunction<T>() {
      @Override
      public boolean equal(T objA, T objB) {
        return true;
      }
    };
  }

  public static class Foo<T> {}

  public static <T> EqualFunction<Foo<T>> createEqualFn(final EqualFunction<T> equalFn) {
    return new EqualFunction<Foo<T>>() {
      @Override
      public boolean equal(Foo<T> first, Foo<T> second) {
        return first == second;
      }
    };
  }
}
