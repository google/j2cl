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
package rawtype;

@SuppressWarnings("rawtypes")
class RawType {
  Unbound rawUnbound = new Unbound();
  Bound rawBound = new Bound();
  BoundRecursively rawBoundRecursively = new BoundRecursively();
  BoundDependentDirect rawBoundDependentDirect = new BoundDependentDirect();
  BoundDependentIndirect rawBoundDependentIndirect = new BoundDependentIndirect();

  static class Unbound<T> {
    T method(T t) {
      return t;
    }
  }

  static class Bound<T extends RawType> {}

  static class BoundRecursively<T extends BoundRecursively<T>> {}

  static class BoundDependentDirect<A, B extends A> {}

  static class BoundDependentIndirect<A, B extends Unbound<A>> {}

  static class RawUnboundChild extends Unbound {
    @Override
    Object method(Object o) {
      return super.method(o);
    }
  }

  interface GenericSuperclass<T extends RawType> {
    default void f(T t) {}
  }

  static class RawSubclass implements GenericSuperclass {
    @Override
    public void f(RawType t) {}
  }
}
