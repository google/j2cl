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
package com.google.j2cl.transpiler.integration.supermethodcall;

public class Child extends Parent {
  @Override
  public String getNameOne() {
    return super.getNameOne();
  }

  @Override
  public int getGrandParentPassedValueTimesTwo(int value) {
    return super.getGrandParentPassedValueTimesTwo(value);
  }

  @Override
  public GrandParent getGrandParentPassedValueWithChangingReturn(GrandParent value) {
    return (GrandParent) super.getGrandParentPassedValueWithChangingReturn(value);
  }

  @Override
  public String getNameTwo() {
    return super.getNameTwo();
  }

  @Override
  public int getParentPassedValue(int value) {
    return super.getParentPassedValue(value);
  }

  @Override
  public Parent getParentPassedValueWithChangingReturn(Parent value) {
    return (Parent) super.getParentPassedValueWithChangingReturn(value);
  }
}
