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

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.RegularTypeDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.UnionTypeDescriptor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Traverses a Type, gathers imports for all things it references and creates
 * non colliding local aliases for each import.
 */
public class ImportGatheringVisitor extends AbstractVisitor {

  /**
   * Enums for describing the category of an import. An eager import is one that should occur in the
   * declaration phase because it provides a supertype. A lazy import should defer to the execution
   * phase so that circular imports are avoided.
   */
  public enum ImportCategory {
    EAGER,
    LAZY
  }

  private Set<RegularTypeDescriptor> typeDescriptorsDefinedInCompilationUnit =
      new LinkedHashSet<>();
  private Map<ImportCategory, Set<RegularTypeDescriptor>> typeDescriptorsByCategory =
      new LinkedHashMap<>();

  public static Map<ImportCategory, Set<Import>> gatherImports(JavaType javaType) {
    return new ImportGatheringVisitor().doGatherImports(javaType);
  }

  @Override
  public void exitTypeDescriptor(TypeDescriptor typeDescriptor) {
    addTypeDescriptor(typeDescriptor, ImportCategory.LAZY);
    for (TypeDescriptor typeArgument : typeDescriptor.getTypeArgumentDescriptors()) {
      addTypeDescriptor(typeArgument, ImportCategory.LAZY);
    }
  }

  @Override
  public void exitFieldDescriptor(FieldDescriptor fieldDescriptor) {
    addTypeDescriptor(fieldDescriptor.getTypeDescriptor(), ImportCategory.LAZY);
    addTypeDescriptor(fieldDescriptor.getEnclosingClassTypeDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitAssertStatement(AssertStatement assertStatement) {
    addTypeDescriptor(TypeDescriptors.VM_ASSERTS_TYPE_DESCRIPTOR, ImportCategory.LAZY);
  }

  @Override
  public void exitJavaType(JavaType type) {
    typeDescriptorsDefinedInCompilationUnit.add(
        (RegularTypeDescriptor) type.getDescriptor().getRawTypeDescriptor());

    // Super type and super interface imports are needed eagerly because they are used during the
    // declaration phase of JS execution. All other imports are lazy.
    if (type.getSuperTypeDescriptor() != null) {
      addTypeDescriptor(type.getSuperTypeDescriptor(), ImportCategory.EAGER);
    }
    for (TypeDescriptor superInterfaceTypeDescriptor : type.getSuperInterfaceTypeDescriptors()) {
      addTypeDescriptor(superInterfaceTypeDescriptor, ImportCategory.EAGER);
    }
  }

  @Override
  public void exitMethod(Method method) {
    TypeDescriptor returnTypeDescriptor = method.getDescriptor().getReturnTypeDescriptor();
    if (!returnTypeDescriptor.isPrimitive()
        || returnTypeDescriptor == TypeDescriptors.LONG_TYPE_DESCRIPTOR) {
      addTypeDescriptor(returnTypeDescriptor, ImportCategory.LAZY);
    }
  }

  @Override
  public void exitExpression(Expression expression) {
    if (TypeDescriptors.LONG_TYPE_DESCRIPTOR == expression.getTypeDescriptor()) {
      // for Long operation method dispatch.
      addLongsTypeDescriptor();
    }
  }

  @Override
  public void exitField(Field field) {
    if (TypeDescriptors.LONG_TYPE_DESCRIPTOR == field.getDescriptor().getTypeDescriptor()) {
      addLongsTypeDescriptor();
    }
  }

  @Override
  public void exitMethodDescriptor(MethodDescriptor methodDescriptor) {
    addTypeDescriptor(methodDescriptor.getEnclosingClassTypeDescriptor(), ImportCategory.LAZY);
    TypeDescriptor returnTypeDescriptor = methodDescriptor.getReturnTypeDescriptor();
    if (needImportForJsDoc(returnTypeDescriptor)) {
      addTypeDescriptor(returnTypeDescriptor, ImportCategory.LAZY);
    }
  }

  @Override
  public void exitUnionTypeDescriptor(UnionTypeDescriptor unionTypeDescriptor) {
    addTypeDescriptor(unionTypeDescriptor, ImportCategory.LAZY);
  }

  private Multiset<String> localNameUses = HashMultiset.create();

  private Map<ImportCategory, Set<Import>> doGatherImports(JavaType javaType) {
    addTypeDescriptor(TypeDescriptors.CLASS_TYPE_DESCRIPTOR, ImportCategory.LAZY);
    addTypeDescriptor(TypeDescriptors.NATIVE_UTIL_TYPE_DESCRIPTOR, ImportCategory.EAGER);

    // Collect type references.
    javaType.accept(this);

    typeDescriptorsByCategory
        .get(ImportCategory.LAZY)
        .removeAll(typeDescriptorsByCategory.get(ImportCategory.EAGER));
    typeDescriptorsByCategory
        .get(ImportCategory.LAZY)
        .removeAll(typeDescriptorsDefinedInCompilationUnit);
    typeDescriptorsByCategory
        .get(ImportCategory.EAGER)
        .removeAll(typeDescriptorsDefinedInCompilationUnit);

    recordLocalNameUses(typeDescriptorsDefinedInCompilationUnit);
    recordLocalNameUses(typeDescriptorsByCategory.get(ImportCategory.LAZY));
    recordLocalNameUses(typeDescriptorsByCategory.get(ImportCategory.EAGER));

    Map<ImportCategory, Set<Import>> importsByCategory = new LinkedHashMap<>();
    importsByCategory.put(
        ImportCategory.LAZY, toImports(typeDescriptorsByCategory.get(ImportCategory.LAZY)));
    importsByCategory.put(
        ImportCategory.EAGER, toImports(typeDescriptorsByCategory.get(ImportCategory.EAGER)));

    return importsByCategory;
  }

  private Set<Import> toImports(Set<RegularTypeDescriptor> typeDescriptors) {
    Set<Import> imports = new LinkedHashSet<>();
    for (TypeDescriptor typeDescriptor : typeDescriptors) {
      Preconditions.checkState(!typeDescriptor.isTypeVariable());
      Preconditions.checkState(!typeDescriptor.isParameterizedType());
      String shortAliasName = getShortAliasName(typeDescriptor);
      int usageCount = localNameUses.count(shortAliasName);
      String aliasName = usageCount == 1 ? shortAliasName : computeLongAliasName(typeDescriptor);
      imports.add(new Import(aliasName, typeDescriptor));
    }
    return imports;
  }

  private void recordLocalNameUses(Set<RegularTypeDescriptor> typeDescriptors) {
    for (TypeDescriptor typeDescriptor : typeDescriptors) {
      localNameUses.add(getShortAliasName(typeDescriptor));
    }
  }

  private void addTypeDescriptor(TypeDescriptor typeDescriptor, ImportCategory importCategory) {
    // Type variables can't be depended upon.
    if (typeDescriptor.isTypeVariable()) {
      return;
    }

    // Special case expand a dependency on the 'long' primitive into a dependency on both the 'long'
    // primitive and the native JS 'Long' emulation class.
    if (TypeDescriptors.LONG_TYPE_DESCRIPTOR == typeDescriptor) {
      typeDescriptorsByCategory
          .get(ImportCategory.EAGER)
          .add((RegularTypeDescriptor) TypeDescriptors.NATIVE_LONG_TYPE_DESCRIPTOR);
      typeDescriptorsByCategory
          .get(importCategory)
          .add((RegularTypeDescriptor) TypeDescriptors.LONG_TYPE_DESCRIPTOR);
      return;
    }

    // Unroll the types inside of a union type.
    if (typeDescriptor instanceof UnionTypeDescriptor) {
      UnionTypeDescriptor unionTypeDescriptor = (UnionTypeDescriptor) typeDescriptor;
      for (TypeDescriptor containedTypeDescriptor : unionTypeDescriptor.getTypes()) {
        addTypeDescriptor(containedTypeDescriptor, importCategory);
      }
      return;
    }

    // Unroll the leaf type in an array type and special case add the native Array utilities.
    if (typeDescriptor instanceof ArrayTypeDescriptor) {
      addTypeDescriptor(TypeDescriptors.VM_ARRAYS_TYPE_DESCRIPTOR, ImportCategory.LAZY);

      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      addTypeDescriptor(arrayTypeDescriptor.getLeafTypeDescriptor(), importCategory);
      return;
    }

    // If there is a type signature like Map<Entry<K, V>> then the Entry type argument needs to be
    // imported.
    if (typeDescriptor.isParameterizedType()) {
      for (TypeDescriptor typeArgumentDescriptor : typeDescriptor.getTypeArgumentDescriptors()) {
        // But the type argument imports do not need to be eager since they are not acting here as
        // super type or super interface.
        addTypeDescriptor(typeArgumentDescriptor, ImportCategory.LAZY);
      }
    }

    typeDescriptorsByCategory
        .get(importCategory)
        .add((RegularTypeDescriptor) typeDescriptor.getRawTypeDescriptor());
  }

  private void addLongsTypeDescriptor() {
    // In particular this import is being done eagerly both because it is safe to do so (the Longs
    // library should not have extended dependencies) but also because the initialization of
    // compile time constant values occurs during the declaration phase and this initialization
    // might use the Longs library $fromString/$fromInt etc.
    addTypeDescriptor(TypeDescriptors.NATIVE_LONGS_TYPE_DESCRIPTOR, ImportCategory.EAGER);
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

  private ImportGatheringVisitor() {
    typeDescriptorsByCategory.put(ImportCategory.EAGER, new LinkedHashSet<RegularTypeDescriptor>());
    typeDescriptorsByCategory.put(ImportCategory.LAZY, new LinkedHashSet<RegularTypeDescriptor>());
  }
}
