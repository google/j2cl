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
package bridgemethods;

import jsinterop.annotations.JsType;

interface SomeInterface<T, S> {
  void foo(T t, S s);

  void foo(T t, Number n);
}

class SuperParent<T, S> {
  public void foo(T t, S s) {}
}

class Parent<T extends Error> extends SuperParent<T, Number> {
  // foo__Error__Number
  // there should be a bridge method foo__Object__Object for SuperParent.foo(T,S),
  // which delegates to foo(Error, Number)
  @Override
  public void foo(T t, Number s) {}

  public <T extends Number> void bar(T t) {} // bar__Number

  public <T> void fun(T t) {}
}

public class BridgeMethod extends Parent<AssertionError>
    implements SomeInterface<AssertionError, Number> {
  // foo__AssertionError__Number, there should be a bridge method
  // foo__Error__Number for Parent.foo(T, Number), and a bridge method
  // foo__Object__Object for SuperParent.foo(T, S) and for SomeInterface.foo(T, S),
  // and a bridge method foo__Object__Number for SomeInterface.foo(T, Number).
  @Override
  public void foo(AssertionError a, Number n) {}

  @J2ktIncompatible // accidental override in Kotlin
  @Override
  public void bar(Number t) {} // no bridge method for Parent.bar(T), same signature.

  public void fun(Number t) {} // not override, no bridge method.
}

interface SomeOtherFooInterface<T> {
  void foo(T t, Double s);
}

class DualUnrelatedBridges implements SomeInterface<String, Double>, SomeOtherFooInterface<String> {
  // foo__Object__Object should be a bridge method
  // foo__Object__Double should be a bridge method
  @Override
  public void foo(String s, Double n) {}

  // foo__Object__Number should be a bridge method
  @Override
  public void foo(String s, Number n) {}
}

class SuperDualUnrelatedAccidentalBridges {
  public void foo(String s, Double n) {}

  public void foo(String s, Number n) {}
}

class DualUnrelatedAccidentalBridges extends SuperDualUnrelatedAccidentalBridges
    implements SomeInterface<String, Double>, SomeOtherFooInterface<String> {
  // foo__Object__Object should be a bridge method
  // foo__Object__Double should be a bridge method
  // foo__Object__Number should be a bridge method
}

interface Getter {
  String get(String s);
}

class ClassWithParameterizedGet<T> {
  public T get(T t) {
    return null;
  }
}

class AccidentalOverrideBridge extends ClassWithParameterizedGet<String> implements Getter {
  // get__String will be a bridge method delegating to (super) get_Object
  // get__Object should be redefined to bridge to get__String
  public void test() {
    Getter g = new AccidentalOverrideBridge();
    g.get("");
  }
}

class TestCase10036 {
  @JsType
  interface BI3 {
    default String get(String value) {
      return "BI3 get String";
    }
  }

  interface BI2 {
    String get(String value);
  }

  interface BI1 extends BI3 {
    @Override
    String get(String value);
  }

  abstract static class B<B1 extends Comparable> implements BI1, BI2 {
    // The abstract method here is unrelated.
    public abstract String get(B1 value);
    // get__String should be a bridge to JsMethod this.get()
    // get should be a default method implemented here calling BI3.get.
  }
}

class TestCase102 {
  @JsType
  interface BI2 {
    void set(String value);
  }

  interface BI1 {
    void set(String value);
  }

  abstract static class B<B1> implements BI1, BI2 {
    public abstract void set(B1 value);
    // get__String should be a bridge to (JsMethod) this.get.
  }

  class C<B1> extends B<B1> {
    @Override
    public void set(String value) {}

    @Override
    public void set(B1 value) {}
  }
}

class ParameterizedParent<T extends ParameterizedParent<T>> {
  <Q extends T> Q m(Q t) {
    return null;
  }
}

class ReparametrerizedChild<E extends ReparametrerizedChild<E>> extends ParameterizedParent<E> {
  <S extends E> S m(S t) {
    return null;
  }
}

class LeafChild extends ReparametrerizedChild<LeafChild> {}

// Bridges that override final methods. Repro for b/280123980.
interface StringConsumer {
  void accept(String s);
}

class Consumer<T> {
  public final void accept(T t) {}
}

// Needs a specializing bridge from accept(String) to super.accept(Object).
class StringConsumerImpl extends Consumer<String> implements StringConsumer {}

// Repro for b/357041082
abstract class SpecializingReturnAbstractClass {
  public abstract Object foo();
}

interface SpecializingReturnInterface {
  String foo();
}

abstract class SpecializingReturnAbstractSubclass extends SpecializingReturnAbstractClass
    implements SpecializingReturnInterface {
  // foo(Object) should be a bridge method.
}

// Repro for b/357043910
interface InterfaceWithDefaultMethod {
  default Object foo() {
    return "A";
  }
}

interface InterfaceOverridingDefaultMethod extends InterfaceWithDefaultMethod {
  String foo();
}

abstract class DoesNotInheritDefaultMethod1
    implements InterfaceWithDefaultMethod, InterfaceOverridingDefaultMethod {}

abstract class DoesNotInheritDefaultMethod2
    implements InterfaceOverridingDefaultMethod, InterfaceWithDefaultMethod {}

class PackagePrivateBridgeSuper<T, U> {
  <S extends T, R extends PackagePrivateBridgeSuper<S, R>> void m(R r, S s, T t, U u) {}
}

final class PackagePrivateBridge<V, W> extends PackagePrivateBridgeSuper<V, W> {
  @Override
  public <S extends V, R extends PackagePrivateBridgeSuper<S, R>> void m(R r, S s, V v, W w) {}
}
