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
package com.google.j2cl.transpiler.backend.common;

import com.google.common.collect.ImmutableSet;

/** Utility that provides handling of recognized annotations. */
public class SupportedAnnotations {

  public static final ImmutableSet<String> COMMON_ANNOTATIONS =
      ImmutableSet.<String>builder()
          // go/keep-sorted start
          .add("com.google.auto.value.AutoValue")
          .add("com.google.auto.value.AutoValue.Builder")
          .add("java.lang.Deprecated")
          .add("java.lang.FunctionalInterface")
          .add("java.lang.SuppressWarnings")
          .add("javaemul.internal.annotations.DoNotAutobox")
          .add("javaemul.internal.annotations.HasNoSideEffects")
          .add("javaemul.internal.annotations.UncheckedCast")
          .add("javaemul.internal.annotations.Wasm")
          .add("javaemul.lang.annotations.WasAutoValue")
          .add("javaemul.lang.annotations.WasAutoValue.Builder")
          .add("jsinterop.annotations.JsIgnore")
          .add("kotlin.Deprecated")
          // go/keep-sorted end
          .build();

  public static final ImmutableSet<String> J2KT_ANNOTATIONS =
      ImmutableSet.<String>builder()
          .addAll(COMMON_ANNOTATIONS)
          // go/keep-sorted start
          .add("com.google.apps.xplat.dagger.asynccomponent.ComponentRegistryCompatible")
          .add("com.google.apps.xplat.testing.parameterized.RunParameterized")
          .add("com.google.common.annotations.J2ktPublic")
          .add("com.google.errorprone.annotations.CanIgnoreReturnValue")
          .add("com.google.errorprone.annotations.ResultIgnorabilityUnspecified")
          .add("com.google.j2kt.annotations.HiddenFromObjC")
          .add("com.google.j2kt.annotations.HidesFromObjC")
          .add("com.google.j2kt.annotations.KtNative")
          .add("com.google.j2kt.annotations.Throws")
          .add("com.google.j2objc.annotations.ObjectiveCKmpMethod")
          .add("com.google.j2objc.annotations.ObjectiveCName")
          .add("com.google.j2objc.annotations.Property")
          .add("com.google.j2objc.annotations.Property.Suppress")
          .add("com.google.j2objc.annotations.SwiftName")
          .add("dagger.Component")
          .add("dagger.Inject")
          .add("dagger.internal.DaggerGenerated")
          .add("javaemul.internal.annotations.KtDisabled")
          .add("javaemul.internal.annotations.KtIn")
          .add("javaemul.internal.annotations.KtName")
          .add("javaemul.internal.annotations.KtNative")
          .add("javaemul.internal.annotations.KtOut")
          .add("javaemul.internal.annotations.KtProperty")
          .add("javax.inject.Inject")
          .add("org.junit.runner.RunWith")
          // go/keep-sorted end
          .build();

  private SupportedAnnotations() {}
}
