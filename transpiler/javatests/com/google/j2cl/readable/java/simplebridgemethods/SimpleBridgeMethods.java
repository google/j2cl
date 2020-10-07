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
package simplebridgemethods;

class Superclass<T extends Error> {
  public void m1(T t) {}
}

// non generic class extends parameterized class, and overrides generic method.
class Subclass extends Superclass<AssertionError> {
  // bridge method: m1(Error a) {m1((AssertionError) a);}
  @Override
  public void m1(AssertionError a) {}
}

// non generic class extends parameterized class, and does not override generic method.
class AnotherSubclass extends Superclass<AssertionError> {
  // there should be no bridge method.
}

interface Callable<V> {
  public void call(V v);
}

// generic class implements parameterized interface, overriding methods without changing signature.
class Task<T> implements Callable<T> {
  // no bridge method, signature does not change.
  @Override
  public void call(T t) {}
}

// generic class implements parameterized interface, overriding methods and changing signature.
class AnotherTask<T extends AssertionError> implements Callable<Superclass<T>> {
  // overrides and changes the signature.
  // bridge method: call(Object) {call((Superclass<T>) t);}
  @Override
  public void call(Superclass<T> t) {}
}

public class SimpleBridgeMethods {}
