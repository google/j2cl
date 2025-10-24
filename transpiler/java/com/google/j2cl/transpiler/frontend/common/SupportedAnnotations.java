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
package com.google.j2cl.transpiler.frontend.common;

import com.google.common.collect.ImmutableSet;

/** Utility that provides handling of recognized annotations. */
public class SupportedAnnotations {

  private static final ImmutableSet<String> RECOGNIZED_ANNOTATIONS_QUALIFIED_NAMES =
      ImmutableSet.of(
          "com.google.apps.xplat.testing.parameterized.RunParameterized",
          "com.google.auto.value.AutoValue",
          "com.google.auto.value.AutoValue.Builder",
          "com.google.j2kt.annotations.HiddenFromObjC",
          "com.google.j2objc.annotations.ObjectiveCName",
          "com.google.j2objc.annotations.SwiftName",
          "java.lang.Deprecated",
          "java.lang.FunctionalInterface",
          "java.lang.SuppressWarnings",
          "javaemul.internal.annotations.DoNotAutobox",
          "javaemul.internal.annotations.HasNoSideEffects",
          "javaemul.internal.annotations.UncheckedCast",
          "javaemul.internal.annotations.Wasm",
          "javaemul.lang.annotations.WasAutoValue",
          "javaemul.lang.annotations.WasAutoValue.Builder",
          "kotlin.Deprecated",
          "org.junit.runner.RunWith");

  public static boolean isSupportedAnnotation(String qualifiedName) {
    return RECOGNIZED_ANNOTATIONS_QUALIFIED_NAMES.contains(qualifiedName);
  }

  private SupportedAnnotations() {}
}
