/*
 * Copyright 2017 Google Inc.
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
package genericequals;

public class GenericEquals<T> {

  private final T value;
  private final GenericEquals<T> value2;

  public GenericEquals(T value) {
    this.value = value;
    this.value2 = null;
  }

  public Object foo(GenericEquals<?> other) {
    return other.value2.value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof GenericEquals)) {
      return false;
    }
    GenericEquals<?> other = (GenericEquals<?>) obj;
    return value.equals(other.value);
  }
}
