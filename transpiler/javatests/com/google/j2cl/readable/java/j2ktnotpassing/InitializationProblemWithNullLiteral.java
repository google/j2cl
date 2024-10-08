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

// TODO(b/372305346): Move to j2kt when fixed.
@NullMarked
public class InitializationProblemWithNullLiteral {
  static class Foo<V extends @Nullable Object> {
    static <V extends @Nullable Object> Foo<V> with(V v) {
      return new Foo<>();
    }
  }

  // Kotlin complains that this field is uninitialized, while the actual problems is hidden in the
  // next field, which uses null literal in generic method invocation, with type inferred as
  // wildcard from assignment.
  final int completelyNormalFinalField;

  // The workaround for this problem specifying explicit type argument:
  // {@code Foo.<@Nullable Void>.with(null)}
  Foo<?> problematicFoo = Foo.with(null);

  InitializationProblemWithNullLiteral() {
    this.completelyNormalFinalField = 0;
  }
}
