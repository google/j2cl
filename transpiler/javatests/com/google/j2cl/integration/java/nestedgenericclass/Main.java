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
package nestedgenericclass;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main<T> {
  public <T> void fun(T outer) {
    /**
     * Generic local class declaring a shadow type variable.
     */
    class A<T> {
      public T t;

      public A(T t) {
        this.t = t;
      }
    }

    /**
     * Non-generic local class that infers type variable declared in enclosing method.
     */
    class B {
      public T t;

      public B(T t) {
        this.t = t;
      }
    }

    // new A<> with different type arguments.
    assertTrue(new A<Error>(new Error()).t instanceof Error);
    assertTrue(new A<Exception>(new Exception()).t instanceof Exception);

    assertTrue((new B(outer).t == outer));
  }

  public static void main(String[] args) {
    Main<String> m = new Main<>();
    // invokes local classes' creation with different types of outer parameters.
    m.fun(new Object());
    m.fun(new Error());
    m.fun(new Exception());
    m.fun(m);
  }
}
