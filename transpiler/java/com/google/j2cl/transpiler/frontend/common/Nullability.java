/*
 * Copyright 2021 Google Inc.
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

/** Utility that provides the handling of recognized nullability annotations. */
public class Nullability {

  public static final String ORG_JSPECIFY_NULLNESS_NULL_MAKED = "org.jspecify.nullness.NullMarked";

  private static final ImmutableSet<String> RECOGNIZED_NULLABLE_ANNOTATIONS_QUALIFIED_NAMES =
      ImmutableSet.of(
          // JsInterop nullable annotation.
          "jsinterop.annotations.JsNullable",
          // Annotations defined by JSpecify.
          "org.jspecify.nullness.Nullable",
          // Annotations supported by Checker Framework. J2CL currently supports the full list of
          // Nullable and NonNull annotations supported by Checker Framework
          // (see https://checkerframework.org/manual/#nullness-related-work). This is important
          // until users stop using the Checker Framework for the static checking. (Otherwise the
          // JavaScript code interacting with Java will not be null-checked correctly).
          "android.annotation.Nullable",
          "android.support.annotation.Nullable",
          "androidx.annotation.Nullable",
          "androidx.annotation.RecentlyNullable",
          "com.sun.istack.internal.Nullable",
          "edu.umd.cs.findbugs.annotations.Nullable",
          "edu.umd.cs.findbugs.annotations.CheckForNull",
          "edu.umd.cs.findbugs.annotations.PossiblyNull",
          "edu.umd.cs.findbugs.annotations.UnknownNullnes",
          "io.reactivex.annotations.Nullable",
          "io.reactivex.rxjava3.annotations.Nullable",
          "javax.annotation.Nullable",
          "javax.annotation.CheckForNull",
          "org.checkerframework.checker.nullness.compatqual.NullableDecl",
          "org.checkerframework.checker.nullness.compatqual.NullableType",
          "org.checkerframework.checker.nullness.qual.Nullable",
          "org.codehaus.commons.nullanalysis.Nullable",
          "org.eclipse.jdt.annotation.Nullable",
          "org.eclipse.jgit.annotations.Nullable",
          "org.jetbrains.annotations.Nullable",
          "org.jmlspecs.annotation.Nullable",
          "org.netbeans.api.annotations.common.NullAllowed",
          "org.netbeans.api.annotations.common.CheckForNull",
          "org.netbeans.api.annotations.common.NullUnknown",
          "org.springframework.lang.Nullable");

  private static final ImmutableSet<String> RECOGNIZED_NON_NULL_ANNOTATIONS_QUALIFIED_NAMES =
      ImmutableSet.of(
          // JsInterop non-null annotation.
          "jsinterop.annotations.JsNonNull",
          // Annotations defined by JSpecify.
          "org.jspecify.nullness.NonNull",
          // Annotations supported by Checker Framework.
          "android.annotation.NonNull",
          "android.support.annotation.NonNull",
          "androidx.annotation.NonNull",
          "androidx.annotation.RecentlyNonNull",
          "com.sun.istack.internal.NotNull",
          "edu.umd.cs.findbugs.annotations.NonNull",
          "io.reactivex.annotations.NonNull",
          "io.reactivex.rxjava3.annotations.NonNull",
          "javax.annotation.Nonnull",
          "javax.validation.constraints.NotNull",
          "lombok.NonNull",
          "org.checkerframework.checker.nullness.compatqual.NonNullDecl",
          "org.checkerframework.checker.nullness.compatqual.NonNullType",
          "org.checkerframework.checker.nullness.qual.NonNull",
          "org.codehaus.commons.nullanalysis.NotNull",
          "org.eclipse.jdt.annotation.NonNull",
          "org.eclipse.jgit.annotations.NonNull",
          "org.jetbrains.annotations.NotNull",
          "org.jmlspecs.annotation.NonNull",
          "org.netbeans.api.annotations.common.NonNull",
          "org.springframework.lang.NonNull");

  public static boolean isNullableAnnotation(String qualifiedName) {
    return RECOGNIZED_NULLABLE_ANNOTATIONS_QUALIFIED_NAMES.contains(qualifiedName);
  }

  public static boolean isNonNullAnnotation(String qualifiedName) {
    return RECOGNIZED_NON_NULL_ANNOTATIONS_QUALIFIED_NAMES.contains(qualifiedName);
  }

  private Nullability() {}
}
