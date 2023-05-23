/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package java.lang;

import javaemul.internal.HashCodes;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Object.html">the
 * official Java API doc</a> for details.
 */
public class Object {

  public int $systemIdentityHashCode; // only used in wasm to store identity hash code.

  public boolean equals(Object that) {
    return this == that;
  }

  public int hashCode() {
    return HashCodes.getObjectIdentityHashCode(this);
  }

  public String toString() {
    return getClass().getName() + "@" + Integer.toHexString(hashCode());
  }

  public final Class<?> getClass() {
    return $getClassImpl();
  }

  // Stub method. Replaced by the compiler.
  public Class<?> $getClassImpl() {
    return null;
  }
}
