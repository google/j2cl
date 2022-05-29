/*
 * Copyright 2022 Google Inc.
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
package javaemul.internal;

/**
 * Defines utility static functions to abstract from the Long emulation library used in JavaScript.
 */
public class LongUtils {

  public static long fromBits(int lowBits, int highBits) {
    return (long) lowBits | (((long) highBits) << 32);
  }

  public static int getHighBits(long valueLong) {
    return (int) (valueLong >> 32);
  }
}
