/*
 * Copyright 2018 Google Inc.
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
package bridgemethods;

interface I<T, S> {
  T fun(S s);
}

class A<T, S> {
  public T fun(S s) {
    return null;
  }

  public T get() {
    return null;
  }
}

class B extends A<Number, String> implements I<Integer, String> {
  // bridge method for A.fun(String):Number and I.fun(String):Integer should both delegate
  // to this method. Since A.fun(String):Number and I.fun(String):Integer has the same signature
  // (although) different return type, only one bridge method should be created.
  public Integer fun(String s) {
    return new Integer(1);
  }
}

/** An interface with the same method as A but specialized. */
interface SpecializedInterface {
  String fun(String s);

  String get();
}

class AccidentalOverride extends A<String, String> implements SpecializedInterface {}
