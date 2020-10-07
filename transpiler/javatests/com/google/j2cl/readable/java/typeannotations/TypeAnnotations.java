/*
 * Copyright 2019 Google Inc.
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
package typeannotations;

import java.util.ArrayList;
import java.util.List;
import jsinterop.annotations.JsNonNull;

abstract class AbstractType implements ParametricType<AbstractType> {
  // The abstract method stubs should take non-null String and list of non-null string respectively.
}

abstract class AbstractTypeNonNull implements ParametricType<@JsNonNull AbstractType> {
  // The abstract method stubs should take non-null String and list of non-null string respectively.
}

class Parent {
  public Parent m(@JsNonNull String s, Parent c) {
    return c;
  }

  public List<@JsNonNull Parent> m(List<@JsNonNull String> l, Parent c) {
    return new ArrayList<>();
  }
}

class ChildWithNullableParent extends Parent implements ParametricType<Parent> {
  // The bridge methods stubs should take non-null String and list of non-null string respectively
  // and return SomeClass an non-null list of SomeClass respectively.
}

class ChildWithNonNullableParent extends Parent implements ParametricType<@JsNonNull Parent> {
  // The bridge methods stubs should take non-null String and list of non-null string respectively
  // and return non-null SomeClass an non-null list of non-null SomeClass respectively.
}

public class TypeAnnotations {
  public static void main() {
    // The ArrayList should be inferred as ArrayList<@JsNonNull String>();
    List<@JsNonNull String> list = new ArrayList<>();
  }
}
