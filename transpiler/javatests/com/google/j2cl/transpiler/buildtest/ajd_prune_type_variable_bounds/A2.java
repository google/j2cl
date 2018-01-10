/*
 * Copyright 2018 Google Inc.
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
package a;

import n.NativeType2;

/** Tests that the indirection by type variable work and n.Native2 is not pruned. */
public abstract class A2<T> {
  public T getType() {
    return null;
  }

  public static <U extends NativeType2, T extends Container<U>> A2<T> newA2() {
    return null;
  }
}
