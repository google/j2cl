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

class ParentOuter {
  public int fieldInParentOuter;

  public void funInParentOuter() {}
}

class ParentInner {
  public int fieldInParentInner;

  public void funInParentInner() {}
}

public class ChildClass extends ParentOuter {
  public int fieldInOuter;

  public void funInOuter() {}

  class InnerClass extends ParentInner {
    public int fieldInInner;

    public void funInInner() {}

    public int testInnerClass() {
      // call to outer class's function inherited from ParentOuter
      funInParentOuter(); // this.outer.funInParentOuter()
      ChildClass.this.funInParentOuter(); // this.outer.funInParentOuter()

      // call to outer class's own declared function.
      funInOuter(); // this.outer.funInOuter()
      ChildClass.this.funInOuter(); // this.outer.funInOuter()

      // call to parent class's function.
      funInParentInner(); // this.funInParentIner()
      this.funInParentInner(); // this.funInParentInner()

      // call to own declared function.
      funInInner(); // this.funInInner()
      this.funInInner(); // this.funInInner()

      // accesses to outer class's own or inherited fields.
      int a = ChildClass.this.fieldInParentOuter;
      a = fieldInParentOuter;
      a = ChildClass.this.fieldInOuter;
      a = fieldInOuter;

      // accesses to its own or inherited fields.
      a = this.fieldInParentInner;
      a = fieldInParentInner;
      a = this.fieldInInner;
      a = fieldInInner;
      return a;
    }
  }

  public void testLocalClass() {
    class LocalClass extends ChildClass {
      Object object = this;

      @Override
      public void funInParentOuter() {}

      public void test() {
        funInOuter(); // this.funInOuter
        ChildClass.this.funInOuter(); // $outer.funInOuter
        funInParentOuter(); // this.funInParentOuter
        this.funInParentOuter(); // this.funInParentOuter
        ChildClass.this.funInParentOuter(); // $outer.funInParentOuter()
      }
    }
    new LocalClass().test();
  }
}
