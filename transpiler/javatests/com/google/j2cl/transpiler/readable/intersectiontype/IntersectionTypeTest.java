package com.google.j2cl.transpiler.readable.intersectiontype;

interface Getable<T> {
  T get();
}

interface Setable {
  void set(int i);
}

interface Serial {}

interface Cmp {
  int cmp();
}

interface Cmp2 {
  int cmp(int a);
}

@SuppressWarnings("unused")
public class IntersectionTypeTest<U> {
  public static <T extends Getable<?> & Setable> void getAndSet(T object) {
    object.set(1);
    object.get();
  }

  class MapEntry {
    public <T> Getable<T> method(Object o) {
      return (Getable<T> & Setable) o;
    }
  }

  public static <T> Getable<T> cast(Object o) {

    // Show that the order does matter.
    if (o == null) {
      return (Setable & Getable<T>) o;
    }
    return (Getable<T> & Setable) o;
  }

  public static <T> Getable<Comparable<String>> cast1(Object s) {
    return (Getable<Comparable<String>> & Setable) s;
  }

  public static <T> Getable<Comparable<T>> cast2(Object s) {
    return (Getable<Comparable<T>> & Setable) s;
  }

  public Object cast3(Object s) {
    return s;
  }

  // TODO: Lambdas do not have the correct types applied yet.  Jdt only recognizes this Lambda as
  // Cmp whereas it should recognize Cmp and Serial.
  // https://bugs.eclipse.org/bugs/show_bug.cgi?id=496596
  public static Cmp method() {
    return (Cmp & Serial) () -> 1;
  }

  // This Lambda is type correctly
  public static Cmp2 method2() {
    return (Cmp2 & Serial) (a) -> 1;
  }

  private static class A {}

  private interface EmptyA {}

  private interface EmptyB {}

  public static void testClosureAssignment(Object o) {
    A e = (A & EmptyA & EmptyB) o;
    EmptyA g = (A & EmptyA & EmptyB) o;
    EmptyB s = (A & EmptyA & EmptyB) o;
  }

  private static <T> T get(T t) {
    return t;
  }

  private static <T extends A & EmptyA> T m() {
    return (T) get(new Object());
  }
}
