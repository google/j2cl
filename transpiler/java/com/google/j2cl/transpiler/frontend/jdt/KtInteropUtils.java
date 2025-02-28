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
import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.isWarningSuppressed;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getJ2ktNativeAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getJ2ktThrowsAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtDisabledAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtInAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtNameAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtNativeAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtObjectiveCNameAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtOutAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtPropertyAnnotation;

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
public class KtInteropUtils {
  private KtInteropUtils() {}

  @Nullable
  public static KtObjcInfo getKtObjcInfo(IBinding binding) {
    IAnnotationBinding annotationBinding = getKtObjectiveCNameAnnotation(binding);
    if (annotationBinding == null) {
      return null;
    }
    return KtObjcInfo.newBuilder()
        .setObjectiveCName(getStringAttribute(annotationBinding, "value"))
        .build();
  }

  @Nullable
  public static KtTypeInfo getKtTypeInfo(IBinding binding) {
    IAnnotationBinding annotationBinding = getKtNativeAnnotation(binding);
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

    annotationBinding = getJ2ktNativeAnnotation(binding);
    if (annotationBinding != null) {
      return KtTypeInfo.newBuilder().build();
    }

    return null;
  }

  public static KtInfo getKtInfo(IMethodBinding methodBinding) {
    return getKtInfo(methodBinding, /* isUninitializedWarningSuppressed= */ false);
  }

  public static KtInfo getKtInfo(IVariableBinding variableBinding) {
    // Checking for both property annotations and enclosing class annotations for uninitialized
    // warning suppressions.
    boolean isUninitializedWarningSuppressed = isUninitializedWarningSuppressed(variableBinding);
    @Nullable ITypeBinding declaringClass = variableBinding.getDeclaringClass();
    while (declaringClass != null && !isUninitializedWarningSuppressed) {
      isUninitializedWarningSuppressed = isUninitializedWarningSuppressed(declaringClass);
      declaringClass = declaringClass.getDeclaringClass();
    }
    return getKtInfo(variableBinding, isUninitializedWarningSuppressed);
  }

  private static KtInfo getKtInfo(IBinding binding, boolean isUninitializedWarningSuppressed) {
    return KtInfo.newBuilder()
        .setProperty(isKtProperty(binding))
        .setName(getKtName(binding))
        .setDisabled(isKtDisabled(binding))
        .setUninitializedWarningSuppressed(isUninitializedWarningSuppressed)
        .setThrows(isThrows(binding))
        .build();
  }

  @Nullable
  private static String getKtName(IBinding binding) {
    IAnnotationBinding annotationBinding = getKtNameAnnotation(binding);
    return annotationBinding != null ? getStringAttribute(annotationBinding, "value") : null;
  }

  private static boolean isKtProperty(IBinding binding) {
    return getKtPropertyAnnotation(binding) != null;
  }

  public static boolean isKtDisabled(IBinding binding) {
    return getKtDisabledAnnotation(binding) != null;
  }

  private static boolean isThrows(IBinding binding) {
    return getJ2ktThrowsAnnotation(binding) != null;
  }

  public static boolean isUninitializedWarningSuppressed(IBinding binding) {
    return isWarningSuppressed(binding, "nullness:initialization.field.uninitialized");
  }

  @Nullable
  public static KtVariance getKtVariance(ITypeBinding typeVariableBinding) {
    if (getKtInAnnotation(typeVariableBinding) != null) {
      return KtVariance.IN;
    } else if (getKtOutAnnotation(typeVariableBinding) != null) {
      return KtVariance.OUT;
    } else {
      return null;
    }
  }
}
