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
package supermethodcall;

interface GrandParentInterface {
  default void defaultGrandParent() {}
}

interface ParentInterface {
  default void defaultParent() {}
}

class GrandParent {
  public void grandParentSimplest() {}

  @SuppressWarnings("unused")
  public void grandParentWithParams(int foo) {}

  public Object grandParentWithChangingReturn() {
    return null;
  }

  public void defaultParent() {}

  public void defaultGrandParent() {}
}

class Parent extends GrandParent implements ParentInterface {
  public void parentSimplest() {}

  @SuppressWarnings("unused")
  public void parentWithParams(int foo) {}

  public Object parentWithChangingReturn() {
    return null;
  }

  @Override
  public void defaultParent() {
    super.defaultParent();
    ParentInterface.super.defaultParent();
  }
}

public class Child extends Parent implements GrandParentInterface {
  @Override
  public void parentSimplest() {
    super.parentSimplest();
  }

  @Override
  public void parentWithParams(int foo) {
    super.parentWithParams(foo);
  }

  @Override
  public Child parentWithChangingReturn() {
    super.parentWithChangingReturn();
    return this;
  }

  @Override
  public void grandParentSimplest() {
    super.grandParentSimplest();
  }

  @Override
  public void grandParentWithParams(int foo) {
    super.grandParentWithParams(foo);
  }

  @Override
  public Child grandParentWithChangingReturn() {
    super.grandParentWithChangingReturn();
    return this;
  }

  @Override
  public void defaultGrandParent() {
    super.defaultGrandParent();
    GrandParentInterface.super.defaultGrandParent();
  }
}

interface I1 {
  default void m() {}
}

interface I2 {
  void m();
}

interface I3 extends I1 {}

class Super {
  public void m() {}
}

class Sub extends Super implements I2, I3 {
  public void m() {
    I3.super.m(); // This should be rendered in Kotlin as super<I3>.m()
  }
}

abstract class SuperToStringTest implements I1 {
  @Override
  public String toString() {
    return super.toString();
  }
}
