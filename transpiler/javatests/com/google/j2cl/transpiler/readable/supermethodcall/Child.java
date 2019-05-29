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
package com.google.j2cl.transpiler.readable.supermethodcall;

class GrandParent {
  public void grandParentSimplest() {}

  @SuppressWarnings("unused")
  public void grandParentWithParams(int foo) {}

  public Object grandParentWithChangingReturn() {
    return null;
  }
}

class Parent extends GrandParent {
  public void parentSimplest() {}

  @SuppressWarnings("unused")
  public void parentWithParams(int foo) {}

  public Object parentWithChangingReturn() {
    return null;
  }
}

public class Child extends Parent {
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
}
