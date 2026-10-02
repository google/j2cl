/*
 * Copyright 2026 Google Inc.
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

// TODO(rluble): Move to j2kt/NullabilityPropagationInOverrides when fixed.
public class NullabilityPropagationFromMultipleOverrides {
  @NullMarked
  public interface NullableFoo {
    @Nullable Object foo();
  }

  @NullMarked
  public interface NonNullFoo {
    Object foo();
  }

  // The return type of foo() needs to be non-null to override both NullableFoo.foo() and
  // NonNullFoo.foo(), regardless of the order of the implemented interfaces.
  public static class NullableFooFirst implements NullableFoo, NonNullFoo {
    @Override
    public Object foo() {
      return new Object();
    }
  }

  public static class NonNullFooFirst implements NonNullFoo, NullableFoo {
    @Override
    public Object foo() {
      return new Object();
    }
  }
}
