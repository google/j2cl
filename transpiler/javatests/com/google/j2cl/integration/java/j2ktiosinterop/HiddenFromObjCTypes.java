/*
 * Copyright 2022 Google Inc.
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

import java.io.IOException;
import java.io.Writer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

// Contains types and members which should be hidden from ObjC because of b/407538927
@NullMarked
public final class HiddenFromObjCTypes {
  public interface Supplier<T extends @Nullable Object> {
    T get();
  }

  public static @Nullable Appendable appendable;
  public static @Nullable StringBuilder stringBuilder;
  public static @Nullable Writer writer;

  public interface GenericWithStringBuilder<T extends @Nullable StringBuilder> {}

  public static StringBuilder returnsStringBuilder(int i) {
    throw new RuntimeException();
  }

  public static void takesStringBuilder(int i, StringBuilder stringBuilder) {
    throw new RuntimeException();
  }

  public static <T extends StringBuilder> void hasStringBuilderParameter(int i, T stringBuilder) {
    throw new RuntimeException();
  }

  public static void hasStringBuilderTypeVariable(int i, Supplier<StringBuilder> stringBuilder) {
    throw new RuntimeException();
  }

  public static void hasStringBuilderWildcard(
      int i, Supplier<? extends StringBuilder> stringBuilder) {
    throw new RuntimeException();
  }

  public static final class CustomAppendable implements Appendable {
    @Override
    public Appendable append(@Nullable CharSequence csq) throws IOException {
      throw new IOException();
    }

    @Override
    public Appendable append(@Nullable CharSequence csq, int start, int end) throws IOException {
      throw new IOException();
    }

    @Override
    public Appendable append(char c) throws IOException {
      throw new IOException();
    }
  }

  public static final class StringBuilderSupplier implements Supplier<StringBuilder> {
    @Override
    public StringBuilder get() {
      throw new AssertionError();
    }
  }

  public static final class ConstructorWithStringBuilder {
    public ConstructorWithStringBuilder(StringBuilder unusedStringBuilder) {}
  }

  public static final class WithToStringOverride {
    @Override
    public String toString() {
      return super.toString();
    }
  }
}
