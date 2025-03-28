/*
 * Copyright 2024 Google Inc.
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
package j2ktnotpassing;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

// Related Kotlin bug: https://youtrack.jetbrains.com/issue/KT-69834
// TODO(b/352407932): Remove when the bug is fixed.
@NullMarked
public final class EnumInitializationProblems {
  // In Kotlin it's not legal to reference static fields in enum constructor calls.
  public enum Enum1 {
    ZERO(Enum1.VALUE);

    Enum1(int value) {}

    private static final int VALUE = 0;
  }

  // In Kotlin it's not legal to call static methods in enum constructor calls.
  public enum Enum2 {
    ZERO(zero());

    Enum2(int value) {}

    private static int zero() {
      return 0;
    }
  }

  // In Kotlin it's not legal to reference already declared values in enum constructor calls, if
  // there are static method which also reference enum values.
  public enum Enum3 {
    ZERO(null),
    ONE(Enum3.ZERO);

    Enum3(@Nullable Enum3 previous) {}

    private static Enum3 zero() {
      return Enum3.ZERO;
    }
  }
}
