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

  public static void testObservable(Observable<?> observable) {
    // TODO(b/261839232): observable needs a cast to Observable<Any?>. However, in this case Java
    // code could be fixed to declare "addObserver(Observer<? super E> observer)".
    observable.addObserver(e -> {});
  }
}
