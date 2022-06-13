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
package enums;

import java.util.function.Function;

public enum Enum1 {
  V1,
  V2
}

enum Enum2 {
  VALUE1(2),
  VALUE2(Enum1.V1),
  VALUE3,
  VALUE4(5) {};

  int foo = Enum1.V1.ordinal();

  Enum2(int foo) {
    this.foo = foo;
  }

  Enum2(Enum foo) {
    this(foo.ordinal());
  }

  Enum2(Object... somePars) {
    this(somePars.length);
  }

  static Enum2 f(Object o) {
    return null;
  }

  static Enum2 C = f(new Object());

  void specialMethods() {
    int o1 = Enum2.valueOf(null).ordinal();
    int o2 = Enum2.valueOf("VALUE1").ordinal();
    int l = Enum2.values().length;
  }
}

enum Enum3 {
  VALUE1(v -> 1 + v);

  Enum3(Function<Integer, Integer> function) {}
}
