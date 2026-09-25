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
  protected String protectedField;

  @SuppressWarnings("nullness:initialization.field.uninitialized")
  String packagePrivateField;

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

  @SuppressWarnings("nullness:initialization.field.uninitialized")
  private class Inner {
    private String unmarkedInnerField;

    private class InnerInner {
      private String unmarkedInnerInnerField;
    }
  }

  public LateInit(T genericValue) {
    init(genericValue);
  }

  private void init(T genericValue) {
    publicField = "public";
    protectedField = "protected";
    packagePrivateField = "packagePrivate";
    privateField = "private";
    genericField = genericValue;
  }

  public String getPublicField() {
    return publicField;
  }

  public void setPublicField(String publicField) {
    this.publicField = publicField;
  }

  String getPackagePrivateField() {
    return packagePrivateField;
  }

  void setPackagePrivateField(String packagePrivateField) {
    this.packagePrivateField = packagePrivateField;
  }

  public static class Sub<T> extends LateInit<T> {
    public Sub(T genericValue) {
      super(genericValue);
    }

    public String getProtectedField() {
      return protectedField;
    }

    public void setProtectedField(String protectedField) {
      this.protectedField = protectedField;
    }
  }

  public int test() {
    return publicField.length()
        + protectedField.length()
        + packagePrivateField.length()
        + privateField.length()
        + initializedField.length()
        + nullableField.length()
        + finalField.length()
        + primitiveField
        + genericField.hashCode();
  }
}
