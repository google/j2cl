/*
 * Copyright 2024 Google Inc.
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
package kotlinjavainterop;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class NullMarkedClass {
  public static Integer getNonNullInteger() {
    return 1;
  }

  public static Boolean getNonNullBoolean() {
    return true;
  }

  public static Character getNonNullCharacter() {
    return 'a';
  }

  public static Short getNonNullShort() {
    return 1;
  }

  public static Byte getNonNullByte() {
    return 1;
  }

  public static Long getNonNullLong() {
    return 1L;
  }

  public static Double getNonNullDouble() {
    return 1.0;
  }

  public static Float getNonNullFloat() {
    return 1.0f;
  }
}
