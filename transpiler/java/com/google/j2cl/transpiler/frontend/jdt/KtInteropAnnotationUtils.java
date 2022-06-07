/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.transpiler.frontend.jdt;

import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_DISABLED_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_NAME_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_NATIVE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_PROPERTY_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.findAnnotationBindingByName;

import org.eclipse.jdt.core.dom.IAnnotationBinding;

/** Utility methods to get information about Kotlin Interop annotations. */
public class KtInteropAnnotationUtils {
  private KtInteropAnnotationUtils() {}

  public static IAnnotationBinding getKtNameAnnotation(IAnnotationBinding[] annotationBindings) {
    return findAnnotationBindingByName(annotationBindings, KT_NAME_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getKtNativeAnnotation(IAnnotationBinding[] annotationBindings) {
    return findAnnotationBindingByName(annotationBindings, KT_NATIVE_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getKtPropertyAnnotation(
      IAnnotationBinding[] annotationBindings) {
    return findAnnotationBindingByName(annotationBindings, KT_PROPERTY_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getKtDisabledAnnotation(
      IAnnotationBinding[] annotationBindings) {
    return findAnnotationBindingByName(annotationBindings, KT_DISABLED_ANNOTATION_NAME);
  }
}
