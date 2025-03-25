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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class MethodReferences<T extends @Nullable Object> {

  interface Producer<T extends @Nullable Object> {
    T produce();
  }

  interface Predicate<T extends @Nullable Object> {
    boolean apply(T parameter);
  }

  interface ArrayProducer<T extends @Nullable Object> {
    T[] produce(int size);
  }

  class ObjectCapturingOuter {
    MethodReferences<T> getMain() {
      return MethodReferences.this;
    }
  }

  interface Function<T extends @Nullable Object, U extends @Nullable Object> {
    U apply(T t);
  }

  interface BiFunction<
      T extends @Nullable Object, U extends @Nullable Object, V extends @Nullable Object> {
    V apply(T t, U u);
  }

  @JsFunction
  interface JsProducer<T extends @Nullable Object> {
    T produce();
  }

  @JsFunction
  interface JsFunctionInterface<T extends @Nullable Object, U extends @Nullable Object> {
    U apply(T t);
  }

  @JsFunction
  interface JsBiFunction<
      T extends @Nullable Object, U extends @Nullable Object, V extends @Nullable Object> {
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

  T t() {
    return null;
  }

  static Producer<String> staticStringProducer = m()::toString;

  Boolean sameAs(Number n) {
    return false;
  }

  private static <U extends @Nullable Object, V extends @Nullable Object>
      void acceptFunctionSuperVariance(Function<? super U, V> f) {}

  void main() {
    Producer<Object> objectFactory = Object::new;
    // Static method
    objectFactory = MethodReferences::m;

    // Qualified instance method
    objectFactory = new MethodReferences<T>()::isA;

    MethodReferences<String> parameterizedInstance = new MethodReferences<String>();
    Producer<String> parameterizedFactory = parameterizedInstance::t;

    BiFunction<MethodReferences<T>, Number, Boolean> biFunction = MethodReferences::sameAs;

    Function<Number, Boolean> functionWithParameters = this::sameAs;

    // Unqualified instance method
    Predicate<MethodReferences<T>> objectPredicate = MethodReferences::isA;

    Producer<ObjectCapturingOuter> objectCapturingOuterProducer = ObjectCapturingOuter::new;

    ArrayProducer<Object> arrayProducer = Double[]::new;

    objectFactory = new MethodReferences<>()::self;

    Function<MethodReferences<T>, Object> function = MethodReferences::self;

    Function<Integer, boolean[]> booleanArrayFactory = boolean[]::new;

    Function<Integer, char[]> charArrayFactory = char[]::new;

    Function<Integer, byte[]> byteArrayFactory = byte[]::new;

    Function<Integer, short[]> shortArrayFactory = short[]::new;

    Function<Integer, int[]> intArrayFactory = int[]::new;

    Function<Integer, long[]> longArrayFactory = long[]::new;

    Function<Integer, float[]> floatArrayFactory = float[]::new;

    Function<Integer, double[]> doubleArrayFactory = double[]::new;

    Function<Integer, Object[]> objectArrayFactory = Object[]::new;

    Function<Integer, @Nullable Object[]> nullableObjectArrayFactory = Object[]::new;

    Function<Integer, String[]> stringArrayFactory = String[]::new;

    Function<Integer, @Nullable String[]> nullableStringArrayFactory = String[]::new;

    Producer<String> superToStringProducer = super::toString;

    JsProducer<Object> jsobjectFactory = Object::new;
    jsobjectFactory = MethodReferences::m;

    jsobjectFactory = new MethodReferences<>()::self;

    JsFunctionInterface<MethodReferences<T>, Object> jsfunction = MethodReferences::self;

    JsFunctionInterface<Integer, Object[]> jsarrayFactory = Object[]::new;

    JsProducer<String> jsSuperToStringProducer = super::toString;

    JsBiFunction<MethodReferences<T>, Number, Boolean> jsbiFunction = MethodReferences::sameAs;

    JsFunctionInterface<Number, Boolean> jsFunctionWithParameters = this::sameAs;

    // These are all redundant casts that should be removable, even after the method references is
    // rewritten as a lambda.
    objectFactory = (Producer<Object>) ((Object) ((Producer<?>) MethodReferences::m));

    // super-type variance
    acceptFunctionSuperVariance(MethodReferences<Object>::self);
  }
}
