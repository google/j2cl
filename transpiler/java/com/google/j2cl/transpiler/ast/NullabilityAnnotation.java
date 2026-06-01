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
package com.google.j2cl.transpiler.ast;

import com.google.common.collect.Comparators;

/** Represents nullability annotations or the absence of. */
public enum NullabilityAnnotation {
  // Ordered from least to most nullable for comparison purposes.
  NOT_NULLABLE,
  NONE,
  NULLABLE;

  public final String toTypeModifierString() {
    return switch (this) {
      case NULLABLE -> "?";
      case NOT_NULLABLE -> "!";
      case NONE -> "";
    };
  }

  public static NullabilityAnnotation mostNullable(
      NullabilityAnnotation first, NullabilityAnnotation second) {
    return Comparators.max(first, second);
  }

  public static NullabilityAnnotation leastNullable(
      NullabilityAnnotation first, NullabilityAnnotation second) {
    return Comparators.min(first, second);
  }

  public boolean isAssignableTo(NullabilityAnnotation other) {
    return compareTo(other) <= 0;
  }
}
