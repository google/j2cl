/*
 * Copyright 2026 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableSet;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.ArrayList;
import java.util.List;

/**
 * Optimize standard (Non-@JvmRecord) Kotlin data classes using {@code ValueType} to reduce their
 * code size.
 */
public class OptimizeStandardDataClass extends LibraryNormalizationPass {

  @Override
  public void applyTo(Library library) {
    library
        .streamTypes()
        .filter(t -> isOptimizableStandardDataClass(t.getDeclaration()))
        .forEach(type ->
            ValueTypeOptimizer.optimizeAsValueType(
                type, getObjectMethodsMask(type), getExcludedFields(type)));
  }

  private static int getObjectMethodsMask(Type type) {
    // RemoveDataClassSyntheticOverrides already stripped kotlinc-synthesized Object methods in the
    // frontend, so any Object method still declared on the type is user-provided.
    int mask = ValueTypeOptimizer.ALL_METHODS_MASK;
    for (MethodDescriptor method : type.getDeclaration().getDeclaredMethodDescriptors()) {
      if (method.isOrOverridesJavaLangObjectMethod()) {
        mask &= ~ValueTypeOptimizer.getMethodMask(method);
      }
    }
    return mask;
  }

  /**
   * Returns the list of fields that should be excluded from the {@code ValueType} mixin.
   *
   * <p>This includes all non-primary constructor fields and all fields from the supertype.
   */
  private static List<FieldDescriptor> getExcludedFields(Type type) {
    ImmutableSet<String> primaryConstructorParameterNames =
        type.getPrimaryConstructor().getParameters().stream()
            .map(Variable::getName)
            .collect(toImmutableSet());
    TypeDeclaration typeDeclaration = type.getDeclaration();
    List<FieldDescriptor> excludedFields = new ArrayList<>();
    for (FieldDescriptor field : ValueTypeOptimizer.getInstanceFields(typeDeclaration)) {
      if (!primaryConstructorParameterNames.contains(field.getName())) {
        excludedFields.add(field);
      }
    }
    excludedFields.addAll(
        ValueTypeOptimizer.getAllInstanceFields(typeDeclaration.getSuperTypeDeclaration()));
    return excludedFields;
  }

  private static boolean isOptimizableStandardDataClass(TypeDeclaration typeDeclaration) {
    // TODO(b/582872366): Unify the optimizability check in this pass and
    // `RemoveDataClassSyntheticOverrides`.
    return typeDeclaration.isKotlinDataClass()
        // @JvmRecord data classes already extend java.lang.Record (which extends ValueType) and are
        // handled by ImplementRecordClasses.
        && !typeDeclaration.isJavaRecord()
        // TODO(b/576822823): Support local data classes (whose synthetic capture fields are added
        // later by ResolveCaptures).
        && !typeDeclaration.isLocal()
        // TODO(b/576824526): Support data classes that extend a superclass other than Object.
        // kotlinc does not synthesize overrides for Object methods declared final in a superclass,
        // so getObjectMethodsMask would need to exclude them from the ValueType mixin.
        && TypeDescriptors.isJavaLangObject(
            typeDeclaration.getSuperTypeDeclaration().toRawTypeDescriptor());
  }
}
