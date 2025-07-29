/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
package lambdas;

public final class JavaInterfaces {
  interface EmptyInterface {}

  interface GenericInterfaceWithSam<T> {
    T foo(T val);
  }

  interface InterfaceWithSamFoo {
    String foo(String val);
  }

  interface InterfaceWithSamBar {
    String bar(String val);
  }

  interface A extends GenericInterfaceWithSam<String> {}

  interface B extends GenericInterfaceWithSam<String>, InterfaceWithSamFoo {}

  interface C extends EmptyInterface, InterfaceWithSamBar {}

  interface D extends InterfaceWithSamFoo {
    String foo(String val);
  }

  interface E {
    String a(String val);

    default void b() {}
  }

  interface F extends InterfaceWithSamFoo {
    default void b() {}
  }

  interface NotSamA {}

  interface NotSamB extends GenericInterfaceWithSam<String>, InterfaceWithSamBar {}

  interface NotSamC extends EmptyInterface {}

  interface NotSamD extends NotSamB {
    void buzz();
  }

  interface NotSamE extends InterfaceWithSamBar, InterfaceWithSamFoo {}

  interface NotSamF {
    void a();

    void b();
  }
}
