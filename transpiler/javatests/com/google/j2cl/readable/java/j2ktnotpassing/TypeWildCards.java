/*
 * Copyright 2023 Google Inc.
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
package j2ktnotpassing;

public class TypeWildCards {
  interface Observer<E> {
    void on(E event);
  }

  interface Observable<E> {
    void addObserver(Observer<E> observer);
  }

  interface RecursiveObservable<E extends RecursiveObservable<E>> {
    void addObserver(Observer<E> observer);
  }

  public static <T extends Observable<?>> void testObservableParameterized(T observable) {
    // TODO(b/261839232): "Expected Nothing" issue
    observable.addObserver(e -> {});
  }

  public static void testRecursiveObservable(RecursiveObservable<?> observable) {
    // TODO(b/261839232): In this case, even a cast RecursiveObservable<Any?> does not help.
    // In this case the only solution is to refactor Java code to:
    // "addObserver(Observer<? super E> observer)".
    observable.addObserver(e -> {});
  }
}
