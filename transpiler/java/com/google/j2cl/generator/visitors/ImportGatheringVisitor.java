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
package com.google.j2cl.generator.visitors;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.UnionTypeDescriptor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

/**
 * Traverses a CompilationUnit and gathers imports for all its referenced types.
 */
public class ImportGatheringVisitor extends AbstractVisitor {
  private Set<Import> importModules = new LinkedHashSet<>();
  private Set<TypeDescriptor> typeDescriptors = new LinkedHashSet<>();
  private Set<TypeDescriptor> typeDescriptorsDefinedInCompilationUnit = new LinkedHashSet<>();

  public static Set<Import> gatherImports(CompilationUnit compilationUnit) {
    return new ImportGatheringVisitor().doGatherImports(compilationUnit);
  }

  @Override
  public void exitArrayTypeDescriptor(ArrayTypeDescriptor arrayTypeDescriptor) {
    importModules.add(Import.IMPORT_VM_ARRAYS);
    return;
  }

  @Override
  public void exitTypeDescriptor(TypeDescriptor typeDescriptor) {
    addTypeDescriptor(typeDescriptor);
  }

  @Override
  public void exitFieldDescriptor(FieldDescriptor fieldDescriptor) {
    addTypeDescriptor(fieldDescriptor.getType());
    addTypeDescriptor(fieldDescriptor.getEnclosingClassDescriptor());
  }

  @Override
  public void exitAssertStatement(AssertStatement assertStatement) {
    importModules.add(Import.IMPORT_VM_ASSERTS);
  }

  @Override
  public void exitCastExpression(CastExpression castExpression) {
    if (castExpression.getCastType() instanceof ArrayTypeDescriptor) {
      // Arrays.$castTo()
      importModules.add(Import.IMPORT_VM_ARRAYS);
      return;
    }
    // Casts.$to()
    importModules.add(Import.IMPORT_VM_CASTS);
  }

  @Override
  public void exitJavaType(JavaType type) {
    typeDescriptorsDefinedInCompilationUnit.add(type.getDescriptor());
  }

  @Override
  public void exitNewArray(NewArray newArray) {
    importModules.add(Import.IMPORT_VM_ARRAYS);
  }

  @Override
  public void exitMethodDescriptor(MethodDescriptor methodDescriptor) {
    typeDescriptors.add(methodDescriptor.getEnclosingClassDescriptor());
  }

  @Override
  public void exitUnionTypeDescriptor(UnionTypeDescriptor unionTypeDescriptor) {
    for (TypeDescriptor typeRef : unionTypeDescriptor.getTypes()) {
      typeDescriptors.add(typeRef);
    }
  }

  private Set<Import> doGatherImports(CompilationUnit compilationUnit) {
    // Collect type references.
    compilationUnit.accept(this);
    Set<TypeDescriptor> importTypeDescriptors =
        new TreeSet<>(Sets.difference(typeDescriptors, typeDescriptorsDefinedInCompilationUnit));
    importModules.addAll(
        FluentIterable.from(importTypeDescriptors)
            .transform(
                new Function<TypeDescriptor, Import>() {
                  @Nullable
                  @Override
                  public Import apply(TypeDescriptor typeDescriptor) {
                    return new Import(typeDescriptor);
                  }
                })
            .toList());
    return importModules;
  }

  private void addTypeDescriptor(TypeDescriptor typeDescriptor) {
    typeDescriptors.add(typeDescriptor);
  }

  private ImportGatheringVisitor() {}
}
