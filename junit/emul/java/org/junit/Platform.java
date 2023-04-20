/*
 * Copyright 2023 Google Inc.
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

package org.junit;

/** Collection of helpers that need a different implementation for j2kt-native. */
final class Platform {

  private Platform() {}

  /**
   * Returns true if the {@code instance} is assignable to the type {@code clazz}. In J2CL, {@code
   * clazz} can only be a concrete type, not an interface. For the purposes of {@link
   * #expectThrows}, this is sufficient, since exceptions are never interfaces.
   */
  static boolean isInstanceOfType(Object instance, Class<?> clazz) {
    String className = clazz.getName();
    for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
      if (type.getName().equals(className)) {
        return true;
      }
    }
    return false;
  }
}
