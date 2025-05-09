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

@NullMarked
public class NullabilityConversion {
  public interface Parent {}

  public interface Child extends Parent {}

  public interface Generic<T extends @Nullable Parent> {}

  public interface ParentNullableBound<T extends @Nullable Object> {}

  public interface ChildNonNullBounds<T extends Object> extends ParentNullableBound<T> {}

  public interface Supplier<T extends @Nullable Child> {
    T get();
  }

  public static class Tests {

    public static class Unions {
      public abstract static class ExceptionNonNull1 extends RuntimeException
          implements Supplier<Child> {}

      public abstract static class ExceptionNullable1 extends RuntimeException
          implements Supplier<@Nullable Child> {}

      public static Child typeArgumentMixedToNonNull() {
        try {
          throw new RuntimeException();
        } catch (ExceptionNonNull1 | ExceptionNullable1 e) {
          // TODO(b/361769898): Missing cast.
          return e.get();
        }
      }
    }

    public static class Raw {
      public static ParentNullableBound nonNullToNullable(ChildNonNullBounds x) {
        return x;
      }
    }
  }
}
