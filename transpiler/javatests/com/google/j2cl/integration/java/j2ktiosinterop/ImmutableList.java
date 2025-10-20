/*
 * Copyright 2025 Google Inc.
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
package j2ktiosinterop;

import java.util.AbstractList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ImmutableList<T extends @Nullable Object> extends AbstractList<T> {

  private ImmutableList() {}

  public static <T extends @Nullable Object> ImmutableList<T> of() {
    return new ImmutableList<>();
  }

  public static <T extends @Nullable Object> ImmutableList<T> of(T e1) {
    return new ImmutableList<>();
  }

  public static <T extends @Nullable Object> ImmutableList<T> of(T e1, T e2) {
    return new ImmutableList<>();
  }

  public static <T extends @Nullable Object> ImmutableList<T> of(T... elements) {
    return new ImmutableList<>();
  }

  public static <T extends @Nullable Object> ImmutableList<T> copyOf(Iterable<T> iterable) {
    return new ImmutableList<>();
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public T get(int index) {
    throw new RuntimeException();
  }

  public static <T extends @Nullable Object> Builder<T> builder() {
    return new Builder<>();
  }

  public static final class Builder<T extends @Nullable Object> {
    private Builder() {}

    public Builder<T> add(T element) {
      return this;
    }

    ImmutableList<T> build() {
      return new ImmutableList<>();
    }
  }
}
