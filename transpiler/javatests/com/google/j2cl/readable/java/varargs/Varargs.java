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
package varargs;

import java.io.Serializable;

public class Varargs {
  interface FunctionalInterface {
    void apply(String... strings);
  }

  private Object[] args;

  public Varargs(int... args) {}

  public Varargs() {
    this(1);
  }

  public void test(int a, Object... args) {}

  public void testCloneable(Cloneable... args) {}

  public void testSerializable(Serializable... args) {}

  public void testAssignment(Object... args) {
    this.args = args;
  }

  public void testLambda(FunctionalInterface functionalInterface) {}

  public static <T> void fun(T... elements) {}

  public static <E> void bar(E a, E b) {
    fun(a, b);
  }

  public void main() {
    Varargs v = new Varargs();
    v.test(1);
    v.test(1, new Object());
    v.test(1, new Object[] {new Object()});
    v.test(1, new Object[] {});
    v.test(1, new Object[][] {});
    v.testCloneable(new Object[][] {});
    v.testSerializable(new Object[][] {});
    v.testLambda(it -> args = it);
  }
}

class Child extends Varargs {
  public Child() {
    super(1);
  }
}
