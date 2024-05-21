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
package accidentaloverride;

class Parent<T extends Error> {
  @SuppressWarnings("unused")
  public void foo(T e) {}

  @SuppressWarnings("unused")
  public final void bar(T e) {}
}

interface SuperInterface<T> {
  void foo(T t);

  void bar(T e);
}

/**
 * Class that subclasses a parameterized type with argument that is not the type bound of the
 * type variable declared in super class.
 */
class AnotherAccidentalOverride extends Parent<AssertionError>
    implements SuperInterface<AssertionError> {
  // there should be a bridge method foo__Object for SuperInterface.foo(T), and the bridge method
  // delegates to Parent.foo__Error().
}

/**
 * Class that subclasses a parameterized type with argument that is the bound of the type variable
 * declared in super class.
 */
public class AccidentalOverride extends Parent<Error> implements SuperInterface<Error> {
  // there should be a bridge method foo__Object for SuperInterface.foo(T), and the bridge
  // method delegates to Parent.foo__Error().
}
