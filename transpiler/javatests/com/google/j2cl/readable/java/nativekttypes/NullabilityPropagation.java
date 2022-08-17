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

public final class NullabilityPropagation {
  interface NullabilityToPropagate {
    @KtPropagateNullability
    String nullableReturnType1();

    @KtPropagateNullability
    String nullableReturnType2();

    @KtPropagateNullability
    @JsNonNull
    String nonNullReturnType1();

    @KtPropagateNullability
    @JsNonNull
    String nonNullReturnType2();

    @KtPropagateNullability
    void nullableParameter1(String s);

    @KtPropagateNullability
    void nullableParameter2(String s);

    @KtPropagateNullability
    void nonNullParameter1(@JsNonNull String s);

    @KtPropagateNullability
    void nonNullParameter2(@JsNonNull String s);
  }

  class Subtype implements NullabilityToPropagate {
    @Override
    public String nullableReturnType1() {
      return "";
    }

    @Override
    public @JsNonNull String nullableReturnType2() {
      return "";
    }

    @Override
    public String nonNullReturnType1() {
      return "";
    }

    @Override
    public @JsNonNull String nonNullReturnType2() {
      return "";
    }

    @Override
    public void nullableParameter1(String s) {}

    @Override
    public void nullableParameter2(@JsNonNull String s) {}

    @Override
    public void nonNullParameter1(String s) {}

    @Override
    public void nonNullParameter2(@JsNonNull String s) {}
  }
}
