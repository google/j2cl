/*
 * Copyright 2014 Google Inc.
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
package com.google.j2cl.benchmarks.octane.deltablue;

/** Strengths are used to measure the relative importance of constraints. */
enum Strength {
  REQUIRED(0),
  STRONG_PREFERRED(1),
  PREFERRED(2),
  STRONG_DEFAULT(3),
  NORMAL(4),
  WEAK_DEFAULT(5),
  WEAKEST(6);

  static boolean stronger(Strength s1, Strength s2) {
    return s1.strengthValue < s2.strengthValue;
  }

  static boolean weaker(Strength s1, Strength s2) {
    return s1.strengthValue > s2.strengthValue;
  }

  static Strength weakestOf(Strength s1, Strength s2) {
    return weaker(s1, s2) ? s1 : s2;
  }

  private final int strengthValue;

  Strength(int strengthValue) {
    this.strengthValue = strengthValue;
  }

  Strength nextWeaker() {
    switch (strengthValue) {
      case 0:
        return Strength.WEAKEST;
      case 1:
        return Strength.WEAK_DEFAULT;
      case 2:
        return Strength.NORMAL;
      case 3:
        return Strength.STRONG_DEFAULT;
      case 4:
        return Strength.PREFERRED;
      case 5:
        return Strength.REQUIRED;
      default:
        throw new AssertionError("Unknown strength.");
    }
  }
}
