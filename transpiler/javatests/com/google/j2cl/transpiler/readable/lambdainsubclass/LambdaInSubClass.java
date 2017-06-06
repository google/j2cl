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
package com.google.j2cl.transpiler.readable.lambdainsubclass;

interface LambdaInterface {
  int foo(int a);
}

public class LambdaInSubClass extends Parent {
  public void test() {
    LambdaInterface l =
        (i -> {
          // call to outer class's inherited function
          funInParent(); // this.outer.funInParent()
          this.funInParent(); // this.outer.funInParent()
          LambdaInSubClass.this.funInParent(); // this.outer.funInParent()

          // access to outer class's inherited fields
          int a = fieldInParent;
          a = this.fieldInParent;
          a = LambdaInSubClass.this.fieldInParent;
          return a;
        });
  }
}
