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
package com.google.j2cl.transpiler.frontend.jdt;

import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.getStringAttribute;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtNativeAnnotation;

import com.google.j2cl.transpiler.ast.KtTypeInfo;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/** Utility functions for Kotlin Interop properties. */
public class KtInteropUtils {
  private KtInteropUtils() {}

  public static KtTypeInfo getKtTypeInfo(ITypeBinding typeBinding) {
    IAnnotationBinding annotationBinding = getKtNativeAnnotation(typeBinding);
    if (annotationBinding == null) {
      return null;
    }

    String qualifiedName = getStringAttribute(annotationBinding, "value");
    return KtTypeInfo.newBuilder().setQualifiedName(qualifiedName).build();
  }
}
