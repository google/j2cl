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
package nativekttypes;

import jsinterop.annotations.JsNonNull;
import jsinterop.annotations.JsOptional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

public final class NullabilityPropagation {
  @NullMarked
  interface NullabilityToPropagate<ExtendsNullable extends @Nullable Object, ExtendsNonNull> {
    @Nullable String nullableString1(@Nullable String s);

    @Nullable String nullableString2(@Nullable String s);

    String nonNullString1(String s);

    String nonNullString2(String s);

    @Nullable ExtendsNullable nullableExtendsNullable1(@Nullable ExtendsNullable s);

    @Nullable ExtendsNullable nullableExtendsNullable2(@Nullable ExtendsNullable s);

    ExtendsNullable nonNullExtendsNullable1(ExtendsNullable s);

    ExtendsNullable nonNullExtendsNullable2(ExtendsNullable s);

    @Nullable ExtendsNonNull nullableExtendsNonNull1(@Nullable ExtendsNonNull s);

    @Nullable ExtendsNonNull nullableExtendsNonNull2(@Nullable ExtendsNonNull s);

    ExtendsNonNull nonNullExtendsNonNull1(ExtendsNonNull s);

    ExtendsNonNull nonNullExtendsNonNull2(ExtendsNonNull s);

    String nonNullStringTransitive(String s);

    String nonNullStringDoubleOverride(String s);

    String jsOptionalParameter(@Nullable @JsOptional String s);
  }

  @NullMarked
  interface Interface {
    String nonNullStringDoubleOverride(String s);
  }

  class Subtype implements NullabilityToPropagate<String, String>, Interface {
    @Override
    public String nullableString1(String s) {
      return s;
    }

    @Override
    public @JsNonNull String nullableString2(@JsNonNull String s) {
      return s;
    }

    @Override
    public String nonNullString1(String s) {
      return s;
    }

    @Override
    public @JsNonNull String nonNullString2(@JsNonNull String s) {
      return s;
    }

    @Override
    public String nullableExtendsNullable1(String s) {
      return s;
    }

    @Override
    public @JsNonNull String nullableExtendsNullable2(@JsNonNull String s) {
      return s;
    }

    @Override
    public String nonNullExtendsNullable1(String s) {
      return s;
    }

    @Override
    public @JsNonNull String nonNullExtendsNullable2(@JsNonNull String s) {
      return s;
    }

    @Override
    public String nullableExtendsNonNull1(String s) {
      return s;
    }

    @Override
    public @JsNonNull String nullableExtendsNonNull2(@JsNonNull String s) {
      return s;
    }

    @Override
    public String nonNullExtendsNonNull1(String s) {
      return s;
    }

    @Override
    public @JsNonNull String nonNullExtendsNonNull2(@JsNonNull String s) {
      return s;
    }

    @Override
    public String nonNullStringTransitive(String s) {
      return s;
    }

    @Override
    public String nonNullStringDoubleOverride(String s) {
      return s;
    }

    @Override
    public String jsOptionalParameter(@JsOptional String s) {
      return s;
    }
  }

  class SubSubType extends Subtype {
    @Override
    public String nonNullStringTransitive(String s) {
      return s;
    }
  }
}
