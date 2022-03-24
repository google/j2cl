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
package jstypevarargs;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class QualifiedSuperMethodCall extends Main {
  public QualifiedSuperMethodCall() {
    super(1);
  }

  public class InnerClass {
    public int test1() {
      // multiple arguments.
      return QualifiedSuperMethodCall.super.f3(1, 1, 2);
    }

    public int test2() {
      // no argument for varargs.
      return QualifiedSuperMethodCall.super.f3(1);
    }

    public int test3() {
      // array literal for varargs.
      return QualifiedSuperMethodCall.super.f3(1, new int[] {1, 2});
    }

    public int test4() {
      // empty array literal for varargs.
      return QualifiedSuperMethodCall.super.f3(1, new int[] {});
    }

    public int test5() {
      // array object for varargs.
      int[] ints = new int[] {1, 2};
      return QualifiedSuperMethodCall.super.f3(1, ints);
    }
  }

  public static void test() {
    InnerClass i = new QualifiedSuperMethodCall().new InnerClass();
    assertTrue(i.test1() == 4);
    assertTrue(i.test2() == 1);
    assertTrue(i.test3() == 4);
    assertTrue(i.test4() == 1);
    assertTrue(i.test5() == 4);
  }
}
