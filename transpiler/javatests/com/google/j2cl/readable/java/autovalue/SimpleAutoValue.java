/*
 * Copyright 2020 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package autovalue;

import com.google.auto.value.AutoValue;
import java.util.AbstractCollection;
import java.util.List;
import javax.annotation.Nullable;
import jsinterop.annotations.JsNonNull;

@AutoValue
public abstract class SimpleAutoValue {
  public abstract int getIntField();

  public abstract boolean getBooleanField();

  public abstract String getStringField();

  public abstract Double getDoubleField();

  @Nullable
  public abstract Double getNullableField();

  public abstract int[] getArrayField();

  // Potential collision with private field from AutoValue generated code.
  private int intField;

  static SimpleAutoValue create() {
    return new AutoValue_SimpleAutoValue(42, true, "text", 43.0, 44.0, new int[] {45});
  }

  private AutoValue_SimpleAutoValue field1;
  @JsNonNull private AutoValue_SimpleAutoValue field2;

  static AutoValue_SimpleAutoValue[] castAndInstanceOf(Object o) {
    return o instanceof AutoValue_SimpleAutoValue[] ? (AutoValue_SimpleAutoValue[]) o : null;
  }

  abstract static class GenericType extends AbstractCollection<AutoValue_SimpleAutoValue> {

    <T extends AutoValue_SimpleAutoValue> T foo(
        List<? extends AutoValue_SimpleAutoValue> o1, List<AutoValue_SimpleAutoValue> o2) {
      foo(null, null).getArrayField();
      AutoValue_SimpleAutoValue o = o1.get(0);
      return null;
    }
  }
}
