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
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JsTypeAnnotation;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.common.TimingCollector;
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
   * Enums for describing the category of an import.
   *
   * <p>An EAGE import is one that should occur in the declaration phase because it provides a
   * supertype.
   *
   * <p>An EXTERN import should only be emitted as the creation of a type alias.
   *
   * <p>A LAZY import should defer to the execution phase so that circular imports are avoided.
   *
   * <p>The SELF is not emitted and only exists to ensure that an alias is created for the current
   * type.
   */
  public enum ImportCategory {
    EAGER,
    EXTERN,
    LAZY,
    SELF
  }

  private static String computeLongAliasName(TypeDescriptor typeDescriptor) {
    return typeDescriptor.getBinaryName().replaceAll("_", "__").replaceAll("\\" + ".", "_");
  }

  public static Map<ImportCategory, Set<Import>> gatherImports(JavaType javaType) {
    TimingCollector.get().startSubSample("Import Gathering Visitor");

    Map<ImportCategory, Set<Import>> map = new ImportGatheringVisitor().doGatherImports(javaType);
    TimingCollector.get().endSubSample();
    return map;
  }

  private static String getShortAliasName(TypeDescriptor typeDescriptor) {
    // Add "$" prefix for bootstrap types and extern types.
    if (BootstrapType.typeDescriptors.contains(TypeDescriptors.toNullable(typeDescriptor))
        || typeDescriptor.isExtern()) {
      return "$" + typeDescriptor.getBinaryClassName();
    }
    return typeDescriptor.getBinaryClassName();
  }

  private final Multiset<String> localNameUses = HashMultiset.create();

  private final Multimap<ImportCategory, TypeDescriptor> typeDescriptorsByCategory =
      LinkedHashMultimap.create();

  private ImportGatheringVisitor() {}

  @Override
  public void exitAssertStatement(AssertStatement assertStatement) {
    addTypeDescriptor(BootstrapType.ASSERTS.getDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitField(Field field) {
    addTypeDescriptor(field.getDescriptor().getTypeDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitJavaType(JavaType type) {
    addTypeDescriptor(type.getDescriptor().getRawTypeDescriptor(), ImportCategory.SELF);

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
  public void exitJsTypeAnnotation(JsTypeAnnotation jsTypeAnnotation) {
    addTypeDescriptor(jsTypeAnnotation.getTypeDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitMethod(Method method) {
    TypeDescriptor returnTypeDescriptor = method.getDescriptor().getReturnTypeDescriptor();
    if (!returnTypeDescriptor.isPrimitive()
        || returnTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveLong)) {
      addTypeDescriptor(returnTypeDescriptor, ImportCategory.LAZY);
    }

    for (Variable parameter : method.getParameters()) {
      addTypeDescriptor(parameter.getTypeDescriptor(), ImportCategory.LAZY);
    }
  }

  @Override
  public void exitMethodCall(MethodCall methodCall) {
    if (methodCall.isStaticDispatch()) {
      addTypeDescriptor(
          methodCall.getTarget().getEnclosingClassTypeDescriptor(), ImportCategory.LAZY);
    }
  }

  @Override
  public void exitNewInstance(NewInstance newInstance) {
    addTypeDescriptor(
        newInstance.getTarget().getEnclosingClassTypeDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitNumberLiteral(NumberLiteral numberLiteral) {
    if (TypeDescriptors.get()
        .primitiveLong
        .equalsIgnoreNullability(numberLiteral.getTypeDescriptor())) {
      // for Long operation method dispatch.
      addLongsTypeDescriptor();
    }
  }

  @Override
  public void exitTypeReference(TypeReference typeReference) {
    addTypeDescriptor(typeReference.getReferencedTypeDescriptor(), ImportCategory.LAZY);
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

    typeDescriptorsByCategory.put(importCategory, rawTypeDescriptor);
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
    if (TypeDescriptors.toNonNullable(TypeDescriptors.get().primitiveLong)
        .equals(TypeDescriptors.toNonNullable(typeDescriptor))) {
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
    TimingCollector timingCollector = TimingCollector.get();
    timingCollector.startSubSample("Add default Classes");

    if (javaType.isJsOverlayImplementation()) {
      // The synthesized JsOverlayImpl type should import the native type eagerly.
      addTypeDescriptor(javaType.getNativeTypeDescriptor(), ImportCategory.EAGER);
    }
    // Util class implements some utility functions and does not depend on any other class, always
    // import it eagerly.
    addTypeDescriptor(BootstrapType.NATIVE_UTIL.getDescriptor(), ImportCategory.EAGER);

    // Collect type references.
    timingCollector.startSample("Collect type references");
    javaType.accept(this);

    Preconditions.checkState(typeDescriptorsByCategory.get(ImportCategory.SELF).size() == 1);

    timingCollector.startSample("Remove duplicate references");
    typeDescriptorsByCategory
        .get(ImportCategory.LAZY)
        .removeAll(typeDescriptorsByCategory.get(ImportCategory.EAGER));
    typeDescriptorsByCategory
        .get(ImportCategory.LAZY)
        .removeAll(typeDescriptorsByCategory.get(ImportCategory.SELF));
    typeDescriptorsByCategory
        .get(ImportCategory.EAGER)
        .removeAll(typeDescriptorsByCategory.get(ImportCategory.SELF));

    timingCollector.startSample("Record Local Name Uses");
    recordLocalNameUses((Set<TypeDescriptor>) typeDescriptorsByCategory.get(ImportCategory.SELF));
    recordLocalNameUses((Set<TypeDescriptor>) typeDescriptorsByCategory.get(ImportCategory.LAZY));
    recordLocalNameUses((Set<TypeDescriptor>) typeDescriptorsByCategory.get(ImportCategory.EAGER));
    recordLocalNameUses((Set<TypeDescriptor>) typeDescriptorsByCategory.get(ImportCategory.EXTERN));

    timingCollector.startSample("Convert to imports");
    Map<ImportCategory, Set<Import>> importsByCategory = new LinkedHashMap<>();
    importsByCategory.put(
        ImportCategory.LAZY,
        toImports((Set<TypeDescriptor>) typeDescriptorsByCategory.get(ImportCategory.LAZY)));
    importsByCategory.put(
        ImportCategory.EAGER,
        toImports((Set<TypeDescriptor>) typeDescriptorsByCategory.get(ImportCategory.EAGER)));
    importsByCategory.put(
        ImportCategory.EXTERN,
        toImports((Set<TypeDescriptor>) typeDescriptorsByCategory.get(ImportCategory.EXTERN)));
    importsByCategory.put(
        ImportCategory.SELF,
        toImports((Set<TypeDescriptor>) typeDescriptorsByCategory.get(ImportCategory.SELF)));
    timingCollector.endSubSample();
    return importsByCategory;
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
