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

import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktDisabledAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktInAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktNameAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktNativeAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktObjectiveCNameAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktOutAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktPropertyAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktPublicNativeAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktThrowsAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.getStringAttribute;
import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.isWarningSuppressed;

import com.google.j2cl.transpiler.ast.KtInfo;
import com.google.j2cl.transpiler.ast.KtObjcInfo;
import com.google.j2cl.transpiler.ast.KtTypeInfo;
import com.google.j2cl.transpiler.ast.KtVariance;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

/** Utility functions for Kotlin Interop properties. */
public class J2ktInteropUtils {
  private J2ktInteropUtils() {}

  @Nullable
  public static KtObjcInfo getJ2ktObjcInfo(IBinding binding) {
    IAnnotationBinding annotationBinding = getJ2ktObjectiveCNameAnnotation(binding);
    if (annotationBinding == null) {
      return null;
    }
    return KtObjcInfo.newBuilder()
        .setObjectiveCName(getStringAttribute(annotationBinding, "value"))
        .build();
  }

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

  public static KtInfo getJ2ktInfo(IMethodBinding methodBinding) {
    return getJ2ktInfo(methodBinding, /* isUninitializedWarningSuppressed= */ false);
  }

  public static KtInfo getJ2ktInfo(IVariableBinding variableBinding) {
    // Checking for both property annotations and enclosing class annotations for uninitialized
    // warning suppressions.
    boolean isUninitializedWarningSuppressed = isUninitializedWarningSuppressed(variableBinding);
    @Nullable ITypeBinding declaringClass = variableBinding.getDeclaringClass();
    while (declaringClass != null && !isUninitializedWarningSuppressed) {
      isUninitializedWarningSuppressed = isUninitializedWarningSuppressed(declaringClass);
      declaringClass = declaringClass.getDeclaringClass();
    }
    return getJ2ktInfo(variableBinding, isUninitializedWarningSuppressed);
  }

  private static KtInfo getJ2ktInfo(IBinding binding, boolean isUninitializedWarningSuppressed) {
    return KtInfo.newBuilder()
        .setProperty(isKtProperty(binding))
        .setName(getJ2ktName(binding))
        .setDisabled(isKtDisabled(binding))
        .setUninitializedWarningSuppressed(isUninitializedWarningSuppressed)
        .setThrows(isThrows(binding))
        .build();
  }

  @Nullable
  private static String getJ2ktName(IBinding binding) {
    IAnnotationBinding annotationBinding = getJ2ktNameAnnotation(binding);
    return annotationBinding != null ? getStringAttribute(annotationBinding, "value") : null;
  }

  private static boolean isKtProperty(IBinding binding) {
    return getJ2ktPropertyAnnotation(binding) != null;
  }

  public static boolean isKtDisabled(IBinding binding) {
    return getJ2ktDisabledAnnotation(binding) != null;
  }

  private static boolean isThrows(IBinding binding) {
    return getJ2ktThrowsAnnotation(binding) != null;
  }

  public static boolean isUninitializedWarningSuppressed(IBinding binding) {
    return isWarningSuppressed(binding, "nullness:initialization.field.uninitialized");
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
