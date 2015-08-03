package com.google.j2cl.transpiler.readable.nestedgenericclass;

public class NestedGenericClass<T> {
  // nested generic classes with shadowing type parameters.
  public class A<T> {
    public class B<T> {}
  }

  // nested non-generic classes that refers to the type parameters declared in outer class.
  public class C {
    public T c;

    public class D {
      public T d;
    }
  }

  // local classes
  public <S> void fun(S t) {
    class E<S> {} // generic class
    class F { // non-generic class
      public S f;
    }
    new E<Number>();
    new F();
  }

  // multiple nesting levels
  public <T> void bar() {
    class G<T> {
      public <T> void bar() {
        class H<T> {}
        new H<Number>();
      }
    }
    new G<Error>().bar();
  }

  public void test() {
    NestedGenericClass<Number> n = new NestedGenericClass<>();
    NestedGenericClass<Number>.A<Error> a = n.new A<>();
    NestedGenericClass<Number>.A<Error>.B<Exception> b = a.new B<>();

    n.new C();
    n.new C().new D();
  }
}
