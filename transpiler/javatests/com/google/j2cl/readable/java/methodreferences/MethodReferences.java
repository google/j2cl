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
package methodreferences;

import jsinterop.annotations.JsFunction;

public class MethodReferences<T> {

  interface Producer<T> {
    T produce();
  }

  interface Predicate<T> {
    boolean apply(T parameter);
  }

  interface ArrayProducer<T> {
    T[] produce(int size);
  }

  class ObjectCapturingOuter {

    MethodReferences<T> getMain() {
      return MethodReferences.this;
    }
  }

  interface Function<T, U> {
    U apply(T t);
  }

  interface BiFunction<T, U, V> {
    V apply(T t, U u);
  }

  @JsFunction
  interface JsProducer<T> {
    T produce();
  }

  @JsFunction
  interface JsFunctionInterface<T, U> {
    U apply(T t);
  }

  @JsFunction
  interface JsBiFunction<T, U, V> {
    V apply(T t, U u);
  }

  static Object m() {
    return new Object();
  }

  public Boolean isA() {
    return true;
  }

  Object self() {
    return this;
  }

  static Producer<String> staticStringProducer = m()::toString;

  Boolean sameAs(Number n) {
    return false;
  }

  void main() {
    Producer<Object> objectFactory = Object::new;
    // Static method
    objectFactory = MethodReferences::m;

    // Qualified instance method
    objectFactory = new MethodReferences<T>()::isA;

    BiFunction<MethodReferences<T>, Number, Boolean> biFunction = MethodReferences::sameAs;

    Function<Number, Boolean> functionWithParameters = this::sameAs;

    // Unqualified instance method
    Predicate<MethodReferences<T>> objectPredicate = MethodReferences::isA;

    Producer<ObjectCapturingOuter> objectCapturingOuterProducer = ObjectCapturingOuter::new;

    ArrayProducer<Object> arrayProducer = Double[]::new;

    objectFactory = new MethodReferences<>()::self;

    Function<MethodReferences<T>, Object> function = MethodReferences::self;

    Function<Integer, Object[]> arrayFactory = Object[]::new;

    Producer<String> superToStringProducer = super::toString;

    JsProducer<Object> jsobjectFactory = Object::new;
    jsobjectFactory = MethodReferences::m;

    jsobjectFactory = new MethodReferences<>()::self;

    JsFunctionInterface<MethodReferences<T>, Object> jsfunction = MethodReferences::self;

    JsFunctionInterface<Integer, Object[]> jsarrayFactory = Object[]::new;

    JsProducer<String> jsSuperToStringProducer = super::toString;

    JsBiFunction<MethodReferences<T>, Number, Boolean> jsbiFunction = MethodReferences::sameAs;

    JsFunctionInterface<Number, Boolean> jsFunctionWithParameters = this::sameAs;
  }
}
