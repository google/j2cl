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
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtDisabledAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtInAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtNameAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtNativeAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtObjectiveCNameAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtOutAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtPropertyAnnotation;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtThrowsAnnotation;

import com.google.j2cl.transpiler.ast.KtInfo;
import com.google.j2cl.transpiler.ast.KtObjcInfo;
import com.google.j2cl.transpiler.ast.KtTypeInfo;
import com.google.j2cl.transpiler.ast.KtVariance;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

/** Utility functions for Kotlin Interop properties. */
public class KtInteropUtils {
  private KtInteropUtils() {}

  public static KtObjcInfo getKtObjcInfo(ITypeBinding typeBinding) {
    return getKtObjcInfo(typeBinding.getAnnotations());
  }

  public static KtObjcInfo getKtObjcInfo(IMethodBinding typeBinding) {
    return getKtObjcInfo(typeBinding.getAnnotations());
  }

  @Nullable
  private static KtObjcInfo getKtObjcInfo(IAnnotationBinding[] annotationBindings) {
    IAnnotationBinding annotationBinding = getKtObjectiveCNameAnnotation(annotationBindings);
    if (annotationBinding == null) {
      return null;
    }
    return KtObjcInfo.newBuilder()
        .setObjectiveCName(getStringAttribute(annotationBinding, "value"))
        .build();
  }

  public static KtTypeInfo getKtTypeInfo(ITypeBinding typeBinding) {
    return getKtTypeInfo(typeBinding.getAnnotations());
  }

  @Nullable
  private static KtTypeInfo getKtTypeInfo(IAnnotationBinding[] annotationBindings) {
    IAnnotationBinding annotationBinding = getKtNativeAnnotation(annotationBindings);
    if (annotationBinding == null) {
      return null;
    }

    String qualifiedName = getStringAttribute(annotationBinding, "name");
    String bridgeQualifiedName = getStringAttribute(annotationBinding, "bridgeName");
    String companionObject = getStringAttribute(annotationBinding, "companionName");
    return KtTypeInfo.newBuilder()
        .setQualifiedName(qualifiedName)
        .setBridgeQualifiedName(bridgeQualifiedName)
        .setCompanionQualifiedName(companionObject)
        .build();
  }

  public static KtInfo getKtInfo(IMethodBinding methodBinding) {
    return getKtInfo(methodBinding.getAnnotations());
  }

  public static KtInfo getKtInfo(IVariableBinding variableBinding) {
    return getKtInfo(variableBinding.getAnnotations());
  }

  private static KtInfo getKtInfo(IAnnotationBinding[] annotationBindings) {
    return KtInfo.newBuilder()
        .setProperty(isKtProperty(annotationBindings))
        .setName(getKtName(annotationBindings))
        .setDisabled(isKtDisabled(annotationBindings))
        .setUninitializedWarningSuppressed(isUninitializedWarningSuppressed(annotationBindings))
        .setThrows(isThrows(annotationBindings))
        .build();
  }

  @Nullable
  private static String getKtName(IAnnotationBinding[] annotationBindings) {
    IAnnotationBinding annotationBinding = getKtNameAnnotation(annotationBindings);
    return annotationBinding != null ? getStringAttribute(annotationBinding, "value") : null;
  }

  private static boolean isKtProperty(IAnnotationBinding[] annotationBindings) {
    return getKtPropertyAnnotation(annotationBindings) != null;
  }

  public static boolean isKtDisabled(IAnnotationBinding[] annotationBindings) {
    return getKtDisabledAnnotation(annotationBindings) != null;
  }

  private static boolean isThrows(IAnnotationBinding[] annotationBindings) {
    return getKtThrowsAnnotation(annotationBindings) != null;
  }

  public static boolean isUninitializedWarningSuppressed(IAnnotationBinding[] annotationBindings) {
    return isWarningSuppressed(annotationBindings, "nullness:initialization.field.uninitialized");
  }

  @Nullable
  public static KtVariance getKtVariance(IAnnotationBinding[] annotationBindings) {
    if (getKtInAnnotation(annotationBindings) != null) {
      return KtVariance.IN;
    } else if (getKtOutAnnotation(annotationBindings) != null) {
      return KtVariance.OUT;
    } else {
      return null;
    }
  }
}
