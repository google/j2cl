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
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.UnionTypeDescriptor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Traverses a CompilationUnit and gathers imports for all its referenced types and creates
 * non colliding local aliases for each import.
 */
public class ImportGatheringVisitor extends AbstractVisitor {
  private Set<TypeDescriptor> typeDescriptors = new LinkedHashSet<>();
  private Set<TypeDescriptor> typeDescriptorsDefinedInCompilationUnit = new LinkedHashSet<>();

  public static Set<Import> gatherImports(CompilationUnit compilationUnit) {
    return new ImportGatheringVisitor().doGatherImports(compilationUnit);
  }

  @Override
  public void exitTypeDescriptor(TypeDescriptor typeDescriptor) {
    addTypeDescriptor(typeDescriptor);
    for (TypeDescriptor typeArgument : typeDescriptor.getTypeArgumentDescriptors()) {
      addTypeDescriptor(typeArgument);
    }
    if (TypeDescriptors.LONG_TYPE_DESCRIPTOR == typeDescriptor) {
      // for function parameter JsDoc.
      addTypeDescriptor(TypeDescriptors.NATIVE_LONG_TYPE_DESCRIPTOR);
    }
  }

  @Override
  public void exitFieldDescriptor(FieldDescriptor fieldDescriptor) {
    addTypeDescriptor(fieldDescriptor.getTypeDescriptor());
    addTypeDescriptor(fieldDescriptor.getEnclosingClassTypeDescriptor());
  }

  @Override
  public void exitAssertStatement(AssertStatement assertStatement) {
    addTypeDescriptor(TypeDescriptors.VM_ASSERTS_TYPE_DESCRIPTOR);
  }

  @Override
  public void exitJavaType(JavaType type) {
    typeDescriptorsDefinedInCompilationUnit.add(type.getDescriptor().getRawTypeDescriptor());
  }

  @Override
  public void exitExpression(Expression expression) {
    if (TypeDescriptors.LONG_TYPE_DESCRIPTOR == expression.getTypeDescriptor()) {
      // for Long operation method dispatch.
      addTypeDescriptor(TypeDescriptors.NATIVE_LONGS_TYPE_DESCRIPTOR);
    }
  }

  @Override
  public void exitField(Field field) {
    if (TypeDescriptors.LONG_TYPE_DESCRIPTOR == field.getDescriptor().getTypeDescriptor()) {
      // for default initial value of Longs.$fromInt(0).
      addTypeDescriptor(TypeDescriptors.NATIVE_LONGS_TYPE_DESCRIPTOR);
    }
  }

  @Override
  public void exitMethodDescriptor(MethodDescriptor methodDescriptor) {
    addTypeDescriptor(methodDescriptor.getEnclosingClassTypeDescriptor());
    TypeDescriptor returnTypeDescriptor = methodDescriptor.getReturnTypeDescriptor();
    if (needImportForJsDoc(returnTypeDescriptor)) {
      addTypeDescriptor(returnTypeDescriptor);
    }
  }

  @Override
  public void exitUnionTypeDescriptor(UnionTypeDescriptor unionTypeDescriptor) {
    for (TypeDescriptor typeDescriptor : unionTypeDescriptor.getTypes()) {
      addTypeDescriptor(typeDescriptor);
    }
  }

  private Set<Import> doGatherImports(CompilationUnit compilationUnit) {
    addTypeDescriptor(TypeDescriptors.CLASS_TYPE_DESCRIPTOR);
    addTypeDescriptor(TypeDescriptors.NATIVE_UTIL_TYPE_DESCRIPTOR);

    HashMultiset.create();

    // Collect type references.
    compilationUnit.accept(this);
    Set<TypeDescriptor> importTypeDescriptors =
        new TreeSet<>(Sets.difference(typeDescriptors, typeDescriptorsDefinedInCompilationUnit));

    final Multiset<String> localNameUses =
        ImmutableMultiset.copyOf(
            FluentIterable.from(
                    Sets.union(typeDescriptorsDefinedInCompilationUnit, importTypeDescriptors))
                .transform(
                    new Function<TypeDescriptor, String>() {
                      @Override
                      public String apply(TypeDescriptor typeDescriptor) {
                        return getShortAliasName(typeDescriptor);
                      }
                    }));

    return new TreeSet<>(
        FluentIterable.from(importTypeDescriptors)
            .transform(
                new Function<TypeDescriptor, Import>() {
                  @Override
                  public Import apply(TypeDescriptor typeDescriptor) {

                    String shortAliasName = getShortAliasName(typeDescriptor);
                    int usageCount = localNameUses.count(shortAliasName);
                    String aliasName =
                        usageCount == 1 ? shortAliasName : computeLongAliasName(typeDescriptor);
                    return new Import(aliasName, typeDescriptor);
                  }
                })
            .toList());
  }

  private void addTypeDescriptor(TypeDescriptor typeDescriptor) {
    if (TypeDescriptors.LONG_TYPE_DESCRIPTOR == typeDescriptor) {
      addTypeDescriptor(TypeDescriptors.NATIVE_LONG_TYPE_DESCRIPTOR);
    }
    if (typeDescriptor.isTypeVariable()) {
      return;
    }
    TypeDescriptor rawTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
    if (rawTypeDescriptor.isArray()) {
      addTypeDescriptor(TypeDescriptors.VM_ARRAYS_TYPE_DESCRIPTOR);
      addTypeDescriptor(rawTypeDescriptor.getLeafTypeDescriptor());
    } else {
      typeDescriptors.add(rawTypeDescriptor);
    }
  }

  private static String computeLongAliasName(TypeDescriptor typeDescriptor) {
    return typeDescriptor.getBinaryName().replaceAll("_", "__").replaceAll("\\" + ".", "_");
  }

  private static String getShortAliasName(TypeDescriptor typeDescriptor) {
    return TypeDescriptors.bootstrapTypeDescriptors.contains(typeDescriptor)
        ? "$" + typeDescriptor.getClassName()
        : typeDescriptor.getClassName();
  }

  private static boolean needImportForJsDoc(TypeDescriptor returnTypeDescriptor) {
    return !returnTypeDescriptor.isPrimitive()
        && returnTypeDescriptor != TypeDescriptors.STRING_TYPE_DESCRIPTOR;
  }

  private ImportGatheringVisitor() {}
}
