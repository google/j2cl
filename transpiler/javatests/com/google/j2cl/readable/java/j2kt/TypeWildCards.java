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
}
