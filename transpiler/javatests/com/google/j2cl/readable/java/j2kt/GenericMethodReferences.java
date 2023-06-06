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
package j2kt;

public class GenericMethodReferences {

  static <T> T staticGenericMethod(T t) {
    return t;
  }

  <T> T instanceGenericMethod(T t) {
    return t;
  }

  void accept(InterfaceWithGenericMethod fn) {}

  void test(GenericMethodReferences instance) {
    accept(GenericMethodReferences::staticGenericMethod);
    accept(this::instanceGenericMethod);
    accept(instance::instanceGenericMethod);
  }
}
