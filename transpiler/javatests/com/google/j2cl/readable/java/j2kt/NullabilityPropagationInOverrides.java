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
package j2kt;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

public class NullabilityPropagationInOverrides {
  @NullMarked
  public static class NullMarkedWithNullableClone {
    public @Nullable Object clone() throws CloneNotSupportedException {
      return null;
    }
  }

  public interface EmptyInterface {}

  // The nullability of clone() is propagated from NullMarkedWithNullableClone.clone().
  public static class ChildOfNullMarked extends NullMarkedWithNullableClone {
    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }

  // Implementing an interface should not change where the nullability of clone() is propagated
  // from, i.e. it should not be propagated from java.lang.Object.clone().
  public static class ChildOfNullMarkedWithInterface extends NullMarkedWithNullableClone
      implements EmptyInterface {
    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }
}
