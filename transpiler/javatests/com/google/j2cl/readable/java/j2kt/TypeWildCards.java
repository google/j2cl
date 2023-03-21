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
package j2kt;

import javaemul.internal.annotations.KtIn;

// TODO(b/202428351): Move to typewildcards readable when generics are fully supported.
class TypeWildCards {
  static class Parent {}

  static class Child extends Parent {}

  interface SupplierWithUpperBound<T extends Parent> {
    T get();
  }

  static void testSupplierWithUpperAndLowerBounds(SupplierWithUpperBound<? super Child> supplier) {
    // Upper bound in Supplier should be respected so assignment to Parent should be possible.
    Parent parent = supplier.get();
  }

  interface Observer<E> {
    void on(E event);
  }

  interface KtInObserver<@KtIn E> {
    void on(E event);
  }

  interface Observable<E> {
    void addObserver(Observer<E> observer);
  }

  interface SuperWildcardObservable<E> {
    void addObserver(Observer<? super E> observer);
  }

  interface KtInObservable<E> {
    void addObserver(KtInObserver<E> observer);
  }

  interface RecursiveObservable<E extends RecursiveObservable<E>> {
    void addObserver(Observer<E> observer);
  }

  static class ObserverHolder<E> {
    Observer<E> observer;
  }

  public static void testObservable(Observable<?> observable) {
    observable.addObserver(e -> {});
  }

  public static <T extends Observable<?>> void testObservableParameterized(T observable) {
    // TODO(b/261839232): "Expected Nothing" issue
    // observable.addObserver(e -> {});
  }

  public static void testSuperWildcardObservable(SuperWildcardObservable<?> observable) {
    observable.addObserver(e -> {});
  }

  public static void testKtInObservable(KtInObservable<?> observable) {
    observable.addObserver(e -> {});
  }

  public static void testRecursiveObservable(RecursiveObservable<?> observable) {
    // TODO(b/261839232): No idea how to convert it to a correct Kotlin code.
    // observable.addObserver(e -> {});
  }

  public static void testObserverHolder(ObserverHolder<?> observerHolder) {
    observerHolder.observer = e -> {};
  }
}
