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
package com.google.j2cl.transpiler.integration.genericinterfaceabstractimpl;

interface I<T> {
  int fun(T t);

  int bar(T t);
}

abstract class A<T extends Number> implements I<T> {
  @Override
  public int bar(T t) {
    return t.intValue();
  }

  // int fun(T) should be added otherwise a JSCompiler warning.
}

abstract class B implements I<Integer> {
  @Override
  public int bar(Integer i) {
    return i.intValue();
  }

  // int fun(T) should be added otherwise a JSCompiler warning.
}

class SubA extends A<Integer> {
  @Override
  public int fun(Integer t) {
    return t.intValue();
  }
}

class SubB extends B {
  @Override
  public int fun(Integer t) {
    return t.intValue();
  }
}

public class Main {
  public static void main(String... args) {
    SubA subA = new SubA();
    SubB subB = new SubB();
    assert (subA.bar(new Integer(1)) == 1);
    assert (subA.fun(new Integer(2)) == 2);
    assert (subB.bar(new Integer(3)) == 3);
    assert (subB.fun(new Integer(4)) == 4);
  }
}
