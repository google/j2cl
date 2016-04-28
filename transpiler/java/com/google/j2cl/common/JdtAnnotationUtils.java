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
package com.google.j2cl.common;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;

/**
 * Utility functions to process JDT annotations.
 */
public class JdtAnnotationUtils {
  public static IAnnotationBinding findAnnotationBindingByName(
      IAnnotationBinding[] annotations, String name) {
    if (annotations == null) {
      return null;
    }
    for (IAnnotationBinding annotationBinding : annotations) {
      if (annotationBinding.getAnnotationType().getQualifiedName().equals(name)) {
        return annotationBinding;
      }
    }
    return null;
  }

  public static String getAnnotationParameterString(
      IAnnotationBinding annotationBinding, String paramName) {
    if (annotationBinding == null) {
      return null;
    }
    for (IMemberValuePairBinding member : annotationBinding.getDeclaredMemberValuePairs()) {
      if (paramName.equals(member.getName())) {
        return (String) member.getValue();
      }
    }
    return null;
  }

  public static Object[] getAnnotationParameterArray(
      IAnnotationBinding annotationBinding, String paramName) {
    if (annotationBinding == null) {
      return null;
    }
    for (IMemberValuePairBinding member : annotationBinding.getDeclaredMemberValuePairs()) {
      if (paramName.equals(member.getName())) {
        if (member.getValue() instanceof Object[]) {
          return (Object[]) member.getValue();
        }
      }
    }
    return null;
  }

  public static boolean getAnnotationParameterBoolean(
      IAnnotationBinding annotationBinding, String paramName, boolean defaultValue) {
    if (annotationBinding == null) {
      return defaultValue;
    }
    for (IMemberValuePairBinding member : annotationBinding.getDeclaredMemberValuePairs()) {
      if (paramName.equals(member.getName())) {
        return (Boolean) member.getValue();
      }
    }
    return defaultValue;
  }
}
