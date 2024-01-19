/*
 * Copyright 2024 Google Inc.
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

// TODO(b/319620723): Move to j2kt readable when the bug is fixed.
// Kotlin requires explicit overload conflict resolution for method calls with empty varargs.
class VarargOverloads {
  static class NewInstanceOverload {
    NewInstanceOverload(int foo, Object... objects) {}

    NewInstanceOverload(int foo, String... strings) {}

    static void test() {
      new NewInstanceOverload(0);
    }
  }

  static class ThisConstructorCallOverload {
    ThisConstructorCallOverload(Object... objects) {}

    ThisConstructorCallOverload(String... strings) {}

    ThisConstructorCallOverload(int foo) {
      this();
    }
  }

  static class SuperConstructorCallOverload {
    SuperConstructorCallOverload(Object... objects) {}

    SuperConstructorCallOverload(String... strings) {}

    static class Explicit extends SuperConstructorCallOverload {
      Explicit() {
        super();
      }
    }

    static class Implicit extends SuperConstructorCallOverload {
      Implicit() {}
    }

    static class FromImplicitConstructor extends SuperConstructorCallOverload {}
  }

  static class MethodCallOverload {
    void method(Object... objects) {}

    void method(String... strings) {}

    void test() {
      method();
    }
  }

  static class MethodCallOverloadInSubtype {
    void method(Object... objects) {}

    static class Subtype extends MethodCallOverloadInSubtype {
      void method(String... strings) {}

      void test() {
        method();
      }
    }
  }

  static class MethodCallVarargAndArrayArgs {
    void method(Object... objects) {}

    void method(String[] strings) {}

    void test() {
      method();
    }
  }

  static class StaticMethodOverloadSubtypeCallTarget {
    static void method(Object... objects) {}

    static void method(String[] strings) {}

    static void generalizedInSubtype(String... strings) {}

    static class Subtype extends StaticMethodOverloadSubtypeCallTarget {
      static void generalizedInSubtype(Object... objects) {}
    }

    void test() {
      Subtype.method();
      Subtype.generalizedInSubtype();
    }
  }

  enum EnumWithOverloadedConstructors {
    CONSTRUCTOR_OVERLOAD,
    SUPER_CONSTRUCTOR_OVERLOAD {};

    EnumWithOverloadedConstructors(Object... objects) {}

    EnumWithOverloadedConstructors(String... strings) {}
  }
}
