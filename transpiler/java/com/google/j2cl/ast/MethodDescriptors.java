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
package com.google.j2cl.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class holding method descriptor creation utilities.
 */
public class MethodDescriptors {
  /**
   * Creates a copy of the given method descriptor by adding the provided parameters to its end.
   * <p>
   * Takes care to correctly mirror the update to any contained erased method descriptor version.
   */
  public static MethodDescriptor createModifiedCopy(
      MethodDescriptor methodDescriptor, List<TypeDescriptor> addedParameters) {
    // Add the provided parameters to the end of the existing parameters list.
    List<TypeDescriptor> parameters =
        new ArrayList<>(methodDescriptor.getParameterTypeDescriptors());
    parameters.addAll(addedParameters);

    return MethodDescriptor.create(
        methodDescriptor.isStatic(),
        methodDescriptor.isRaw(),
        methodDescriptor.getVisibility(),
        methodDescriptor.getEnclosingClassTypeDescriptor(),
        methodDescriptor.getMethodName(),
        methodDescriptor.isConstructor(),
        methodDescriptor.isNative(),
        methodDescriptor.getReturnTypeDescriptor(),
        parameters,
        methodDescriptor.getTypeParameterDescriptors());
  }
}
