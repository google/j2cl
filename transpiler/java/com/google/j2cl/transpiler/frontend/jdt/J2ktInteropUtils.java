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

import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktInAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktNativeAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktOutAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktPublicNativeAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.getStringAttribute;

import com.google.j2cl.transpiler.ast.KtTypeInfo;
import com.google.j2cl.transpiler.ast.KtVariance;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/** Utility functions for Kotlin Interop properties. */
public class J2ktInteropUtils {
  private J2ktInteropUtils() {}

  @Nullable
  public static KtTypeInfo getJ2ktTypeInfo(IBinding binding) {
    IAnnotationBinding annotationBinding = getJ2ktNativeAnnotation(binding);
    if (annotationBinding != null) {
      String qualifiedName = getStringAttribute(annotationBinding, "name");
      String bridgeQualifiedName = getStringAttribute(annotationBinding, "bridgeName");
      String companionObject = getStringAttribute(annotationBinding, "companionName");
      return KtTypeInfo.newBuilder()
          .setQualifiedName(qualifiedName)
          .setBridgeQualifiedName(bridgeQualifiedName)
          .setCompanionQualifiedName(companionObject)
          .build();
    }

    annotationBinding = getJ2ktPublicNativeAnnotation(binding);
    if (annotationBinding != null) {
      return KtTypeInfo.newBuilder().build();
    }

    return null;
  }

  @Nullable
  public static KtVariance getJ2ktVariance(ITypeBinding typeVariableBinding) {
    if (getJ2ktInAnnotation(typeVariableBinding) != null) {
      return KtVariance.IN;
    } else if (getJ2ktOutAnnotation(typeVariableBinding) != null) {
      return KtVariance.OUT;
    } else {
      return null;
    }
  }
}
