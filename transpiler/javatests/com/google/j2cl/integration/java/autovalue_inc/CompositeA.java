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
package autovalue_inc;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class CompositeA {
  public abstract int getIntField();

  public abstract boolean getBooleanField();

  public abstract String getStringField();

  public abstract Double getDoubleField();

  public abstract ComponentA getComponentField();
}
