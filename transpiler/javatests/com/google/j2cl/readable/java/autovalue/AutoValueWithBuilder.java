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
import javax.annotation.Nullable;

@AutoValue
public abstract class AutoValueWithBuilder {
  public abstract boolean getBooleanField();

  @Nullable
  public abstract Double getNullableField();

  public abstract Builder toBuilder();

  @AutoValue.Builder
  abstract static class Builder {
    public abstract Builder setBooleanField(boolean x);

    public abstract Builder setNullableField(Double x);

    public abstract AutoValueWithBuilder build();
  }

  static AutoValueWithBuilder create() {
    return new AutoValue_AutoValueWithBuilder.Builder().setBooleanField(true).build();
  }
}
