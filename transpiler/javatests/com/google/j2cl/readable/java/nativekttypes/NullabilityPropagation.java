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

import javaemul.internal.annotations.KtPropagateNullability;
import jsinterop.annotations.JsNonNull;
import org.jspecify.nullness.NullMarked;
import org.jspecify.nullness.Nullable;

public final class NullabilityPropagation {
  @NullMarked
  interface NullabilityToPropagate<ExtendsNullable extends @Nullable Object, ExtendsNonNull> {
    @KtPropagateNullability
    @Nullable String getNullableString1();

    @KtPropagateNullability
    @Nullable String getNullableString2();

    @KtPropagateNullability
    String getNonNullString1();

    @KtPropagateNullability
    String getNonNullString2();

    @KtPropagateNullability
    void setNullableString1(@Nullable String s);

    @KtPropagateNullability
    void setNullableString2(@Nullable String s);

    @KtPropagateNullability
    void setNonNullString1(String s);

    @KtPropagateNullability
    void setNonNullString2(String s);

    @KtPropagateNullability
    @Nullable ExtendsNullable getNullableExtendsNullable1();

    @KtPropagateNullability
    @Nullable ExtendsNullable getNullableExtendsNullable2();

    @KtPropagateNullability
    ExtendsNullable getNonNullExtendsNullable1();

    @KtPropagateNullability
    ExtendsNullable getNonNullExtendsNullable2();

    @KtPropagateNullability
    void setNullableExtendsNullable1(@Nullable ExtendsNullable s);

    @KtPropagateNullability
    void setNullableExtendsNullable2(@Nullable ExtendsNullable s);

    @KtPropagateNullability
    void setNonNullExtendsNullable1(ExtendsNullable s);

    @KtPropagateNullability
    void setNonNullExtendsNullable2(ExtendsNullable s);

    @KtPropagateNullability
    @Nullable ExtendsNonNull getNullableExtendsNonNull1();

    @KtPropagateNullability
    @Nullable ExtendsNonNull getNullableExtendsNonNull2();

    @KtPropagateNullability
    ExtendsNonNull getNonNullExtendsNonNull1();

    @KtPropagateNullability
    ExtendsNonNull getNonNullExtendsNonNull2();

    @KtPropagateNullability
    void setNullableExtendsNonNull1(@Nullable ExtendsNonNull s);

    @KtPropagateNullability
    void setNullableExtendsNonNull2(@Nullable ExtendsNonNull s);

    @KtPropagateNullability
    void setNonNullExtendsNonNull1(ExtendsNonNull s);

    @KtPropagateNullability
    void setNonNullExtendsNonNull2(ExtendsNonNull s);
  }

  class Subtype implements NullabilityToPropagate<String, String> {
    @Override
    public String getNullableString1() {
      return "";
    }

    @Override
    public @JsNonNull String getNullableString2() {
      return "";
    }

    @Override
    public String getNonNullString1() {
      return "";
    }

    @Override
    public @JsNonNull String getNonNullString2() {
      return "";
    }

    @Override
    public void setNullableString1(String s) {}

    @Override
    public void setNullableString2(@JsNonNull String s) {}

    @Override
    public void setNonNullString1(String s) {}

    @Override
    public void setNonNullString2(@JsNonNull String s) {}

    @Override
    public String getNullableExtendsNullable1() {
      return "";
    }

    @Override
    public @JsNonNull String getNullableExtendsNullable2() {
      return "";
    }

    @Override
    public String getNonNullExtendsNullable1() {
      return "";
    }

    @Override
    public @JsNonNull String getNonNullExtendsNullable2() {
      return "";
    }

    @Override
    public void setNullableExtendsNullable1(String s) {}

    @Override
    public void setNullableExtendsNullable2(@JsNonNull String s) {}

    @Override
    public void setNonNullExtendsNullable1(String s) {}

    @Override
    public void setNonNullExtendsNullable2(@JsNonNull String s) {}

    @Override
    public String getNullableExtendsNonNull1() {
      return "";
    }

    @Override
    public @JsNonNull String getNullableExtendsNonNull2() {
      return "";
    }

    @Override
    public String getNonNullExtendsNonNull1() {
      return "";
    }

    @Override
    public @JsNonNull String getNonNullExtendsNonNull2() {
      return "";
    }

    @Override
    public void setNullableExtendsNonNull1(String s) {}

    @Override
    public void setNullableExtendsNonNull2(@JsNonNull String s) {}

    @Override
    public void setNonNullExtendsNonNull1(String s) {}

    @Override
    public void setNonNullExtendsNonNull2(@JsNonNull String s) {}
  }
}
