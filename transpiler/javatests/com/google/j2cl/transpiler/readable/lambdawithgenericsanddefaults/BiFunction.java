package com.google.j2cl.transpiler.readable.lambdawithgenericsanddefaults;

@FunctionalInterface
interface BiFunction<T, U, R> {

  R apply(T t, U u);

  default <V> BiFunction<T, U, V> andThen(Function<? super R, ? extends V> after) {
    return (t, u) -> after.apply(this.apply(t, u));
  }
}

@FunctionalInterface
interface Function<T, R> {

  static <T> Function<T, T> identity() {
    return t -> t;
  }

  R apply(T t);
}
