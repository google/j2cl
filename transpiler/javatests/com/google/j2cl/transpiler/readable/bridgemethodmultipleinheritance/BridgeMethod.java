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
package com.google.j2cl.transpiler.readable.bridgemethodmultipleinheritance;

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
