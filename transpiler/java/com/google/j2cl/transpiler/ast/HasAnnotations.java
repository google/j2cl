/*
 * Copyright 2025 Google Inc.
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

import static com.google.common.collect.MoreCollectors.toOptional;

import com.google.common.collect.ImmutableList;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/** Implemented by nodes that have annotations. */
public interface HasAnnotations {

  /** Gets a list of annotations present on this node. */
  ImmutableList<Annotation> getAnnotations();

  /** Returns whether the declaration has an annotation with the given qualified name. */
  default boolean hasAnnotation(String qualifiedName) {
    return getAnnotation(qualifiedName) != null;
  }

  /** Returns whether the declaration has an annotation matching the specified predicate. */
  default boolean hasAnnotation(Predicate<Annotation> fn) {
    return getAnnotations().stream().anyMatch(fn);
  }

  /**
   * Returns the annotation on this declaration with the given qualified name, or {@code null} if
   * not found. Throws an exception if more than one is found.
   */
  @Nullable
  default Annotation getAnnotation(String qualifiedName) {
    return getAnnotations().stream()
        .filter(
            annotation ->
                annotation.getTypeDescriptor().getQualifiedSourceName().equals(qualifiedName))
        .collect(toOptional())
        .orElse(null);
  }
}
