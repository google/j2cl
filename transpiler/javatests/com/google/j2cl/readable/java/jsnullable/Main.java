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
package jsnullable;

import java.util.ArrayList;
import java.util.List;
import jsinterop.annotations.JsNullable;
import org.jspecify.nullness.NullMarked;

@NullMarked
public class Main {
  private String f1 = "Hello";
  @JsNullable private String f2 = null;
  private List<String> f4 = new ArrayList<>();
  @JsNullable private List<String> f5 = new ArrayList<>();
  private List<@JsNullable String> f6 = new ArrayList<>();
  // Nonnullable array of nullable strings.
  @JsNullable private String[] f9 = {};
  // Nullable array of non-nullable strings.
  private String @JsNullable [] f10 = {};

  private <T> void m(T t, @JsNullable T nullableT) {}
}
