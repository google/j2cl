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
package com.google.j2cl.transpiler.integration.overridingclassmethods;

/**
 * Test overriding class methods.
 */
public class Main {
  public static void main(String... args) {
    Parent parentClass = new Parent();
    assert parentClass.overrideWithNoChange() instanceof Parent;
    assert parentClass.overrideWithReturnChange() instanceof Parent;
    assert parentClass.paramChangeCantOverride(null) instanceof Parent;

    Parent childClass = new Child();
    assert childClass.overrideWithNoChange() instanceof Child;
    assert childClass.overrideWithReturnChange() instanceof Child;
    assert childClass.paramChangeCantOverride(null) instanceof Parent;
  }
}
