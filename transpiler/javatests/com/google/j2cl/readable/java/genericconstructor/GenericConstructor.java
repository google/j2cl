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
package genericconstructor;

public class GenericConstructor<T> {
  // constructor with both method level and class level type parameters.
  public <S> GenericConstructor(S s, T t) {}

  // constructor with method level type parameter that has the same same with the class level one.
  public <T> GenericConstructor(T t) {}

  public void test() {
    new GenericConstructor<Error>(new Exception(), new Error());
    new GenericConstructor<Error>(new Exception());
  }
}
