/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.transpiler.frontend.javac;

import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_OPTIONAL_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_PACKAGE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.findAnnotationByName;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.getAnnotationParameterString;

import com.sun.tools.javac.code.Symbol.MethodSymbol;
import javax.annotation.Nullable;
import javax.lang.model.element.PackageElement;

/** Utility functions for JsInterop properties. */
public final class JsInteropUtils {

  public static boolean isJsOptional(MethodSymbol method, int i) {
    return findAnnotationByName(method.getParameters().get(i), JS_OPTIONAL_ANNOTATION_NAME) != null;
  }

  /** The namespace specified on a package. */
  @Nullable
  public static String getJsNamespace(PackageElement packageElement) {
    return getAnnotationParameterString(
        findAnnotationByName(packageElement, JS_PACKAGE_ANNOTATION_NAME), "namespace");
  }

  private JsInteropUtils() {}
}
