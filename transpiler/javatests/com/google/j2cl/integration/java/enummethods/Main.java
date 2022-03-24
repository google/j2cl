/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package enummethods;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsType;

/** Checks that .name() and .ordinal() are callable from javascript */
public class Main {

  @JsType(isNative = true, namespace = "foo.test")
  private static class Test {
    static native int getOrdinal(Enum<?> someEnum);

    static native String getName(Enum<?> someEnum);

    static native int compare(Enum<?> enumA, Enum<?> enumB);
  }

  private static enum MyEnum {
    A,
    B,
    C;
  }

  public static void main(String... args) {
    assertTrue(Test.getOrdinal(MyEnum.A) == 0);
    assertTrue(Test.getOrdinal(MyEnum.B) == 1);
    assertTrue(Test.getOrdinal(MyEnum.C) == 2);

    assertTrue(Test.getName(MyEnum.A) == "A");
    assertTrue(Test.getName(MyEnum.B) == "B");
    assertTrue(Test.getName(MyEnum.C) == "C");

    assertTrue(Test.compare(MyEnum.A, MyEnum.A) == 0);
    assertTrue(Test.compare(MyEnum.B, MyEnum.A) > 0);
    assertTrue(Test.compare(MyEnum.A, MyEnum.C) < 0);
  }
}
