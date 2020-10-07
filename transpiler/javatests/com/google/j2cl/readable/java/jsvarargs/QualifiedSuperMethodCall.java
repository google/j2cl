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
package jsvarargs;

public class QualifiedSuperMethodCall extends Main {
  public QualifiedSuperMethodCall() {
    super(0);
  }

  public class InnerClass {
    public void test() {
      // multiple arguments.
      QualifiedSuperMethodCall.super.f3(1, 1, 2);
      // no argument for varargs.
      QualifiedSuperMethodCall.super.f3(1);
      // array literal for varargs.
      QualifiedSuperMethodCall.super.f3(1, new int[] {1, 2});
      // empty array literal for varargs.
      QualifiedSuperMethodCall.super.f3(1, new int[] {});
      // array object for varargs.
      int[] ints = new int[] {1, 2};
      QualifiedSuperMethodCall.super.f3(1, ints);
    }
  }
}
