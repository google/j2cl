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
package com.google.j2cl.transpiler.integration.staticnestedclass;

public class Main {
  public static class ParentThis {
    public static class StaticNestedClass {
      public int field = 1;
    }
  }

  public static class ParentThat {
    public static class StaticNestedClass {
      public int field = 2;
    }
  }

  public static void main(String... args) {
    ParentThis.StaticNestedClass thisClass = new ParentThis.StaticNestedClass();
    assert thisClass instanceof ParentThis.StaticNestedClass;
    assert thisClass.field == 1;

    ParentThat.StaticNestedClass thatClass = new ParentThat.StaticNestedClass();
    assert thatClass instanceof ParentThat.StaticNestedClass;
    assert thatClass.field == 2;
  }
}
