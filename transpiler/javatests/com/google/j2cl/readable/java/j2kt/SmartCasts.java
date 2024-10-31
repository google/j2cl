/*
 * Copyright 2024 Google Inc.
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
package j2kt;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SmartCasts {
  public interface Foo<T> {
    T get();

    default void testThisReference() {
      acceptFooOfObject((Foo<Object>) this);
      acceptFooOfT(this);
    }
  }

  void testThisReferenceInConstructor() {
    class Local<T extends Local<T>> {
      final Object fieldInitializedInConstructor;
      T t;

      Local() {
        t = (T) this;
        fieldInitializedInConstructor = 1;
      }
    }
  }

  void testThisReferenceInInitializer() {
    class Local<T extends Local<T>> {
      @NonNull Object fieldInitializedInInitializer;
      T t;

      {
        t = (T) this;
        fieldInitializedInInitializer = 1;
      }
    }
  }

  public static class Container<T> {
    public final Foo<T> foo;
    public Foo<T> nonFinalFoo;

    public Container(Foo<T> foo) {
      this.foo = foo;
      this.nonFinalFoo = foo;
    }

    public Foo<T> getFoo() {
      return foo;
    }

    public void testImplicitThisReference() {
      acceptFooOfObject((Foo<Object>) foo);
      acceptFooOfT(foo);
    }

    public void testExplicitThisReference() {
      acceptFooOfObject((Foo<Object>) this.foo);
      acceptFooOfT(this.foo);
    }

    public void testMixedThisReference() {
      acceptFooOfObject((Foo<Object>) this.foo);
      acceptFooOfT(foo);
    }

    public static class SubContainer<T> extends Container<T> {
      SubContainer(Foo<T> foo) {
        super(foo);
      }

      public void testSuperReference() {
        acceptFooOfObject((Foo<Object>) super.foo);
        acceptFooOfT(super.foo);
      }

      public void testSuperAndThisReference() {
        acceptFooOfObject((Foo<Object>) super.foo);
        acceptFooOfT(this.foo);
      }
    }
  }

  public static <T> void testParameterReference(Foo<T> foo) {
    acceptFooOfObject((Foo<Object>) foo);
    acceptFooOfT(foo);
  }

  public static <T> void testFinalVariableReference(Foo<T> foo) {
    final Foo<T> localFoo = foo;
    acceptFooOfObject((Foo<Object>) localFoo);
    acceptFooOfT(localFoo);
  }

  public static <T> void testNonFinalVariableReference(Foo<T> foo, Foo<T> foo2) {
    Foo<T> localFoo = foo;
    localFoo = foo2;
    acceptFooOfObject((Foo<Object>) localFoo);
    acceptFooOfT(localFoo);
  }

  public static <T> void testArguments(Foo<T> foo) {
    acceptFooOfObjectAndT((Foo<Object>) foo, foo);
  }

  public static <T> void testIfStatement(Foo<T> foo, boolean condition) {
    if (condition) {
      acceptT((Foo<Object>) foo);
    }
    acceptFooOfT(foo);
  }

  public static <T> void testFinalFieldAccess(Container<T> container) {
    acceptFooOfObject((Foo<Object>) container.foo);
    acceptFooOfT(container.foo);
  }

  public static <T> void testFinalFieldAccessThroughCastVariable(Container<T> container) {
    acceptFooOfObject(((Container<Object>) container).foo);
    acceptFooOfT(container.foo);
  }

  public static <T> void testNonFinalFieldAccess(Container<T> container) {
    acceptFooOfObject((Foo<Object>) container.nonFinalFoo);
    acceptFooOfT(container.nonFinalFoo);
  }

  public static <T> void testInvocation(Container<T> container) {
    acceptFooOfObject((Foo<Object>) container.getFoo());
    acceptFooOfT(container.getFoo());
  }

  public static <T> void testTypeArgumentInference(Foo<T> foo) {
    acceptFooOfObject((Foo<Object>) foo);
    acceptT(foo.get());
  }

  public static <T> void testArray(T[] a) {
    acceptArrayOfObject(a);
    acceptArrayOfT(a);
  }

  public static <T> void testVararg(T... a) {
    acceptArrayOfObject(a);
    acceptArrayOfT(a);
  }

  public static void acceptFooOfObject(Foo<Object> foo) {}

  public static <T> void acceptFooOfT(Foo<T> foo) {}

  public static <T> void acceptFooOfObjectAndT(Foo<Object> foo, Foo<T> foo2) {}

  public static <T> void acceptT(T foo) {}

  public static void acceptArrayOfObject(Object[] a) {}

  public static <T> void acceptArrayOfT(T[] a) {}
}
