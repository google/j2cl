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
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Traverses a Type, gathers imports for all things it references and creates non colliding local
 * aliases for each import.
 */
public class ImportGatheringVisitor extends AbstractVisitor {

  /**
   * Enums for describing the category of an import. An eager import is one that should occur in the
   * declaration phase because it provides a supertype. A lazy import should defer to the execution
   * phase so that circular imports are avoided.
   */
  public enum ImportCategory {
    EAGER,
    EXTERN,
    LAZY
  }

  private static String computeLongAliasName(TypeDescriptor typeDescriptor) {
    return typeDescriptor.getBinaryName().replaceAll("_", "__").replaceAll("\\" + ".", "_");
  }

  public static Map<ImportCategory, Set<Import>> gatherImports(JavaType javaType) {
    return new ImportGatheringVisitor().doGatherImports(javaType);
  }

  private static String getShortAliasName(TypeDescriptor typeDescriptor) {
    // Add "$" prefix for bootstrap types and extern types.
    if (BootstrapType.typeDescriptors.contains(TypeDescriptors.toNullable(typeDescriptor))
        || typeDescriptor.isExtern()) {
      return "$" + typeDescriptor.getBinaryClassName();
    }
    return typeDescriptor.getBinaryClassName();
  }

  private static boolean needImportForJsDoc(TypeDescriptor returnTypeDescriptor) {
    return !returnTypeDescriptor.isPrimitive()
        && !returnTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().javaLangString);
  }

  private final Multiset<String> localNameUses = HashMultiset.create();

  private final Map<ImportCategory, Set<TypeDescriptor>> typeDescriptorsByCategory =
      new LinkedHashMap<>();

  private final Set<TypeDescriptor> typeDescriptorsDefinedInCompilationUnit = new LinkedHashSet<>();

  private ImportGatheringVisitor() {
    typeDescriptorsByCategory.put(ImportCategory.EAGER, new LinkedHashSet<TypeDescriptor>());
    typeDescriptorsByCategory.put(ImportCategory.LAZY, new LinkedHashSet<TypeDescriptor>());
    typeDescriptorsByCategory.put(ImportCategory.EXTERN, new LinkedHashSet<TypeDescriptor>());
  }

  private void addLongsTypeDescriptor() {
    // In particular this import is being done eagerly both because it is safe to do so (the Longs
    // library should not have extended dependencies) but also because the initialization of
    // compile time constant values occurs during the declaration phase and this initialization
    // might use the Longs library $fromBits/$fromInt etc.
    addTypeDescriptor(BootstrapType.LONGS.getDescriptor(), ImportCategory.EAGER);
  }

  private void addRawTypeDescriptor(
      ImportCategory importCategory, TypeDescriptor rawTypeDescriptor) {
    Preconditions.checkArgument(rawTypeDescriptor.getTypeArgumentDescriptors().isEmpty());

    if (rawTypeDescriptor.isExtern()) {
      importCategory = ImportCategory.EXTERN;
    }

    typeDescriptorsByCategory.get(importCategory).add(rawTypeDescriptor);
  }

  private void addTypeDescriptor(TypeDescriptor typeDescriptor, ImportCategory importCategory) {
    // The "unknownType" can't be depended upon.
    if (typeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().unknownType)) {
      return;
    }
    // Type variables can't be depended upon.
    if (typeDescriptor.isTypeVariable() || typeDescriptor.isWildCard()) {
      return;
    }

    // Special case expand a dependency on the 'long' primitive into a dependency on both the 'long'
    // primitive and the native JS 'Long' emulation class.
    boolean equalIgnoringNullability =
        TypeDescriptors.toNonNullable(TypeDescriptors.get().primitiveLong)
            .equals(TypeDescriptors.toNonNullable(typeDescriptor));
    if (equalIgnoringNullability) {
      addRawTypeDescriptor(ImportCategory.EAGER, BootstrapType.NATIVE_LONG.getDescriptor());
      addRawTypeDescriptor(importCategory, TypeDescriptors.get().primitiveLong);
      return;
    }

    // Unroll the types inside of a union type.
    if (typeDescriptor.isUnion()) {
      for (TypeDescriptor containedTypeDescriptor : typeDescriptor.getUnionedTypeDescriptors()) {
        addTypeDescriptor(containedTypeDescriptor, importCategory);
      }
      return;
    }

    // Unroll the leaf type in an array type and special case add the native Array utilities.
    if (typeDescriptor.isArray()) {
      addTypeDescriptor(BootstrapType.ARRAYS.getDescriptor(), ImportCategory.LAZY);
      addTypeDescriptor(typeDescriptor.getLeafTypeDescriptor(), importCategory);
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

    mayAddTypeDescriptorsIntroducedByJsFunction(typeDescriptor);
    addRawTypeDescriptor(importCategory, typeDescriptor.getRawTypeDescriptor());
  }

  private Map<ImportCategory, Set<Import>> doGatherImports(JavaType javaType) {
    if (javaType.isJsOverlayImplementation()) {
      // The synthesized JsOverlayImpl type should import the native type eagerly.
      addTypeDescriptor(javaType.getNativeTypeDescriptor(), ImportCategory.EAGER);
    } else {
      // The synthesized JsOverlayImpl type does not need the class literal stuff, thus does not
      // need eagerly import the Class native_boostrap types.
      addTypeDescriptor(TypeDescriptors.get().javaLangClass, ImportCategory.LAZY);
    }
    // Util class implements some utility functions and does not depend on any other class, always
    // import it eagerly.
    addTypeDescriptor(BootstrapType.NATIVE_UTIL.getDescriptor(), ImportCategory.EAGER);

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
    recordLocalNameUses(typeDescriptorsByCategory.get(ImportCategory.EXTERN));

    Map<ImportCategory, Set<Import>> importsByCategory = new LinkedHashMap<>();
    importsByCategory.put(
        ImportCategory.LAZY, toImports(typeDescriptorsByCategory.get(ImportCategory.LAZY)));
    importsByCategory.put(
        ImportCategory.EAGER, toImports(typeDescriptorsByCategory.get(ImportCategory.EAGER)));
    importsByCategory.put(
        ImportCategory.EXTERN, toImports(typeDescriptorsByCategory.get(ImportCategory.EXTERN)));

    return importsByCategory;
  }

  @Override
  public void exitAssertStatement(AssertStatement assertStatement) {
    addTypeDescriptor(BootstrapType.ASSERTS.getDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitExpression(Expression expression) {
    if (TypeDescriptors.get()
        .primitiveLong
        .equalsIgnoreNullability(expression.getTypeDescriptor())) {
      // for Long operation method dispatch.
      addLongsTypeDescriptor();
    }
  }

  @Override
  public void exitField(Field field) {
    if (TypeDescriptors.get()
        .primitiveLong
        .equalsIgnoreNullability(field.getDescriptor().getTypeDescriptor())) {
      addLongsTypeDescriptor();
    }
  }

  @Override
  public void exitFieldDescriptor(FieldDescriptor fieldDescriptor) {
    addTypeDescriptor(fieldDescriptor.getTypeDescriptor(), ImportCategory.LAZY);
    addTypeDescriptor(fieldDescriptor.getEnclosingClassTypeDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitJavaType(JavaType type) {
    typeDescriptorsDefinedInCompilationUnit.add(type.getDescriptor().getRawTypeDescriptor());

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
        || returnTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveLong)) {
      addTypeDescriptor(returnTypeDescriptor, ImportCategory.LAZY);
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
  public void exitTypeDescriptor(TypeDescriptor typeDescriptor) {
    addTypeDescriptor(typeDescriptor, ImportCategory.LAZY);
    for (TypeDescriptor typeArgument : typeDescriptor.getTypeArgumentDescriptors()) {
      addTypeDescriptor(typeArgument, ImportCategory.LAZY);
    }
  }

  /**
   * JsFunction type is annotated as function(Foo):Bar, we need to import the parameter types and
   * return type.
   */
  private void mayAddTypeDescriptorsIntroducedByJsFunction(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isJsFunctionImplementation() || typeDescriptor.isJsFunctionInterface()) {
      if (typeDescriptor.isRawType()) {
        // raw type is emit as window.Function.
        return;
      }
      MethodDescriptor jsFunctionMethodDescriptor =
          typeDescriptor.getConcreteJsFunctionMethodDescriptor();
      if (jsFunctionMethodDescriptor == null) {
        return;
      }
      for (TypeDescriptor parameterTypeDescriptor :
          jsFunctionMethodDescriptor.getParameterTypeDescriptors()) {
        addTypeDescriptor(parameterTypeDescriptor, ImportCategory.LAZY);
      }
      addTypeDescriptor(jsFunctionMethodDescriptor.getReturnTypeDescriptor(), ImportCategory.LAZY);
    }
  }

  private void recordLocalNameUses(Set<TypeDescriptor> typeDescriptors) {
    for (TypeDescriptor typeDescriptor : typeDescriptors) {
      localNameUses.add(getShortAliasName(typeDescriptor));
    }
  }

  private Set<Import> toImports(Set<TypeDescriptor> typeDescriptors) {
    Set<Import> imports = new LinkedHashSet<>();
    for (TypeDescriptor typeDescriptor : typeDescriptors) {
      Preconditions.checkState(!typeDescriptor.isTypeVariable());
      Preconditions.checkState(typeDescriptor.isNative() || !typeDescriptor.isParameterizedType());
      String shortAliasName = getShortAliasName(typeDescriptor);
      int usageCount = localNameUses.count(shortAliasName);
      String aliasName = usageCount == 1 ? shortAliasName : computeLongAliasName(typeDescriptor);
      imports.add(new Import(aliasName, typeDescriptor));
    }
    return imports;
  }
}
