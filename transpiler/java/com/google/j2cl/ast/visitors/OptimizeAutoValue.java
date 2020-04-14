/*
 * Copyright 2020 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptors;

/** Optimize AutoValue generated classes to reduce their code size. */
public class OptimizeAutoValue extends NormalizationPass {

  private final boolean enabled;

  public OptimizeAutoValue(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void applyTo(Type type) {
    if (!enabled) {
      return;
    }

    // Change the parent type of AutoValue class to ValueType.
    if (isOptimizableAutoValue(type.getDeclaration())) {
      type.setSuperTypeDescriptor(TypeDescriptors.get().javaemulValueType);
      return;
    }

    // Remove generated j.l.Object overrides from AutoValue implementation class.

    if (type.getSuperTypeDescriptor() == null
        || !isOptimizableAutoValue(type.getSuperTypeDescriptor().getTypeDeclaration())) {
      return;
    }

    type.getMembers()
        .removeIf(
            m -> m.isMethod() && ((Method) m).getDescriptor().isOrOverridesJavaLangObjectMethod());
  }

  private static boolean isOptimizableAutoValue(TypeDeclaration declaration) {
    return declaration.isAnnotatedWithAutoValue()
        && declaration.getSuperTypeDescriptor() == TypeDescriptors.get().javaLangObject;
  }
}
