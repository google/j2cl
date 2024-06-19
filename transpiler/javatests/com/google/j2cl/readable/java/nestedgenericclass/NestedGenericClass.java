/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nestedgenericclass;

import org.jspecify.annotations.Nullable;

public class NestedGenericClass<T> {
  // nested generic classes with shadowing type parameters.
  public class A<T> {
    public class B<T> {
      B() {}
    }
  }

  // nested non-generic classes that refers to the type parameters declared in outer class.
  public class C {
    public @Nullable T c;

    public class D {
      public @Nullable T d;
    }
  }

  // local classes
  public <S> void fun(S t) {
    class E<S> {} // generic class
    class F { // non-generic class
      public @Nullable S f;
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

  class RecursiveTypeVariable<T extends RecursiveTypeVariable<T>> {
    class Inner {}

    void test(T t) {
      t.new Inner();
    }
  }
}
