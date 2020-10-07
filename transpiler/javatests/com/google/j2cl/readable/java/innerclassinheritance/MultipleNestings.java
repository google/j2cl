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
package innerclassinheritance;

public class MultipleNestings {
  public class Parent {
    public void fun() {}
  }

  public void funInM() {}

  public class InnerClass1 extends Parent {
    public void funInI1() {}

    /**
     * Inner class that extends the same class as its enclosing class extends.
     */
    public class InnerClass2 extends Parent {
      public void funInI2() {}

      public void test() {
        // call to inherited fun() with different qualifier
        this.fun(); // this.fun()
        fun(); // this.fun()
        InnerClass1.this.fun(); // this.outer.fun()

        // call to function of outer classes
        funInM(); // this.outer.outer.funInM()
        MultipleNestings.this.funInM(); // this.outer.outer.funInM()
        funInI1(); // this.outer.funInI1()
        InnerClass1.this.funInI1(); // this.outer.funInI1()

        funInI2(); // this.funInI2();
      }
    }
  }
}
