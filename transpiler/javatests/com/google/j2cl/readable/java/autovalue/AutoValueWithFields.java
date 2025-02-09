/*
 * Copyright 2021 Google Inc.
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
import com.google.auto.value.extension.memoized.Memoized;
import java.io.Serializable;

@AutoValue
abstract class AutoValueWithFields extends Parent implements Serializable {

  // Special cased by AutoValue when class implements Serializable.
  private static final long serialVersionUID = 42L;

  private static final long staticField = 42L;

  int userField2;

  public abstract int getIntField();

  @Memoized
  @J2ktIncompatible // TODO(b/385167941): Remove when @Memoized can be used with J2KT.
  int getMemoizedNative() {
    return getIntField() * 2;
  }
}

class Parent {

  private static final long staticField = 42L;

  int userField1;
}
