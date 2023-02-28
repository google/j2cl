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

  public static final ImmutableSet<String> RECOGNIZED_NULL_MARKED_ANNOTATIONS_QUALIFIED_NAMES =
      ImmutableSet.of(
          "org.jspecify.nullness.NullMarked", // removed in jspecify v0.3.0.
          "org.jspecify.annotations.NullMarked",
          "com.google.protobuf.Internal.ProtoNonnullApi");

  private static final ImmutableSet<String> RECOGNIZED_NULLABLE_ANNOTATIONS_QUALIFIED_NAMES =
      ImmutableSet.of(
          // JsInterop nullable annotation.
          "jsinterop.annotations.JsNullable",
          // Annotations defined by JSpecify.
          "org.jspecify.nullness.Nullable", // removed in jspecify v0.3.0.
          "org.jspecify.annotations.Nullable",
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
          "org.checkerframework.checker.nullness.qual.MonotonicNonNull",
          "org.checkerframework.checker.nullness.qual.Nullable",
          "org.codehaus.commons.nullanalysis.Nullable",
          "org.eclipse.jdt.annotation.Nullable",
          "org.eclipse.jgit.annotations.Nullable",
          "org.jetbrains.annotations.Nullable",
          "org.jmlspecs.annotation.Nullable",
          "org.netbeans.api.annotations.common.NullAllowed",
          "org.netbeans.api.annotations.common.CheckForNull",
          "org.netbeans.api.annotations.common.NullUnknown",
          "org.springframework.lang.Nullable",
          // Other annotations recognized by the JSpecify reference checker:
          "android.support.annotation.RecentlyNullable",
          "com.android.annotations.Nullable",
          "com.beust.jcommander.internal.Nullable",
          "com.google.api.server.spi.config.Nullable",
          "com.google.firebase.database.annotations.Nullable",
          "com.google.firebase.internal.Nullable",
          "com.google.gerrit.common.Nullable",
          "com.google.protobuf.Internal.ProtoMethodAcceptsNullParameter",
          "com.google.protobuf.Internal.ProtoMethodMayReturnNull",
          "com.google.protobuf.Internal.ProtoPassThroughNullness",
          "com.mongodb.lang.Nullable",
          "com.sun.istack.Nullable",
          "com.unboundid.util.Nullable",
          "io.micrometer.core.lang.Nullable",
          "io.micronaut.core.annotation.Nullable",
          "io.vertx.codegen.annotations.Nullable",
          "jakarta.annotation.Nullable",
          "junitparams.converters.Nullable",
          "libcore.util.Nullable",
          "net.bytebuddy.agent.utility.nullability.AlwaysNull",
          "net.bytebuddy.agent.utility.nullability.MaybeNull",
          "net.bytebuddy.utility.nullability.AlwaysNull",
          "net.bytebuddy.utility.nullability.MaybeNull",
          "org.apache.avro.reflect.Nullable",
          "org.apache.cxf.jaxrs.ext.Nullable",
          "org.apache.shindig.common.Nullable",
          "org.json.Nullable", // Used in parts of j2objc instead of libcore.util.Nullable
          "reactor.util.annotation.Nullable");

  private static final ImmutableSet<String> RECOGNIZED_NON_NULL_ANNOTATIONS_QUALIFIED_NAMES =
      ImmutableSet.of(
          // JsInterop non-null annotation.
          "jsinterop.annotations.JsNonNull",
          // Annotations defined by JSpecify.
          "org.jspecify.nullness.NonNull", // removed in jspecify v0.3.0.
          "org.jspecify.annotations.NonNull",
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
          "org.springframework.lang.NonNull",
          // Other annotations recognized by the JSpecify reference checker:
          "com.android.annotations.NonNull",
          "com.google.firebase.database.annotations.NotNull",
          "com.google.firebase.internal.NonNull",
          "com.sun.istack.NotNull",
          "com.unboundid.util.NotNull",
          "io.micrometer.core.lang.NonNull",
          "io.micronaut.core.annotation.NonNull",
          "jakarta.annotation.Nonnull",
          "libcore.util.NonNull",
          "net.bytebuddy.agent.utility.nullability.NeverNull",
          "net.bytebuddy.utility.nullability.NeverNull",
          "org.antlr.v4.runtime.misc.NotNull",
          "org.eclipse.lsp4j.jsonrpc.validation.NonNull",
          "org.json.NonNull", // Used in parts of j2objc instead of libcore.util.NonNull
          "reactor.util.annotation.NonNull");

  public static boolean isNullMarkedAnnotation(String qualifiedName) {
    return RECOGNIZED_NULL_MARKED_ANNOTATIONS_QUALIFIED_NAMES.contains(qualifiedName);
  }

  public static boolean isNullableAnnotation(String qualifiedName) {
    return RECOGNIZED_NULLABLE_ANNOTATIONS_QUALIFIED_NAMES.contains(qualifiedName);
  }

  public static boolean isNonNullAnnotation(String qualifiedName) {
    return RECOGNIZED_NON_NULL_ANNOTATIONS_QUALIFIED_NAMES.contains(qualifiedName);
  }

  private Nullability() {}
}
