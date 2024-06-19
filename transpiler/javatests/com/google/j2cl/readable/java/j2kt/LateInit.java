/*
 * Copyright 2023 Google Inc.
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

@NullMarked
public class LateInit<T> {
  @SuppressWarnings("nullness:initialization.field.uninitialized")
  public String publicField;

  @SuppressWarnings("nullness:initialization.field.uninitialized")
  private String privateField;

  @SuppressWarnings("nullness:initialization.field.uninitialized")
  private String initializedField = "initialized";

  @SuppressWarnings("nullness:initialization.field.uninitialized")
  private @Nullable String nullableField;

  @SuppressWarnings("nullness:initialization.field.uninitialized")
  private final String finalField = "final";

  @SuppressWarnings("nullness:initialization.field.uninitialized")
  private int primitiveField;

  @SuppressWarnings("nullness:initialization.field.uninitialized")
  private T genericField;

  public LateInit(T genericValue) {
    init(genericValue);
  }

  private void init(T genericValue) {
    publicField = "public";
    privateField = "private";
    genericField = genericValue;
  }

  public int test() {
    return publicField.length()
        + privateField.length()
        + initializedField.length()
        + nullableField.length()
        + finalField.length()
        + primitiveField
        + genericField.hashCode();
  }
}
