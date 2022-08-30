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
package interfaces;

public class Main {

  interface Interface<T> {
    public int a = 1;
    public static int b = 2;

    void interfaceMethod();

    default void defaultMethod(T t) {
      this.privateMethod(t);
    }

    private void privateMethod(T t) {}

    static void staticInterfaceMethod() {}
  }

  interface SubInterface extends Interface<String> {
    default void defaultMethod(String s) {
      Interface.super.defaultMethod(s);
    }
  }

  class Implementor implements SubInterface , Interface<String> {
    @Override
    public void interfaceMethod() {
    }
  }

  abstract class AbstractImplementor implements SubInterface {}

  void testInterfaceMembers() {
    Interface<String> i = new Implementor();
    i.interfaceMethod();
    i.defaultMethod(null);
    Interface.staticInterfaceMethod();
    int x = (new Implementor()).a + Interface.b;
  }
}
