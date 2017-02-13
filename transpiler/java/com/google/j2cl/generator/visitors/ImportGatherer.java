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
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.JsDocAnnotatedExpression;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.Type;
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
public class ImportGatherer extends AbstractVisitor {

  /**
   * Enums for describing the category of an import.
   *
   * <p>An EAGER import is one that should occur in the declaration phase because it provides a
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

  public static Map<ImportCategory, Set<Import>> gatherImports(Type type) {
    TimingCollector.get().startSubSample("Import Gathering Visitor");

    Map<ImportCategory, Set<Import>> map = new ImportGatherer().doGatherImports(type);
    TimingCollector.get().endSubSample();
    return map;
  }

  private final Multiset<String> localNameUses = HashMultiset.create();

  private final Multimap<ImportCategory, TypeDescriptor> typeDescriptorsByCategory =
      LinkedHashMultimap.create();

  private ImportGatherer() {}

  @Override
  public void exitAssertStatement(AssertStatement assertStatement) {
    addTypeDescriptor(BootstrapType.ASSERTS.getDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitField(Field field) {
    addTypeDescriptor(field.getDescriptor().getTypeDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitFunctionExpression(FunctionExpression functionExpression) {
    for (Variable parameter : functionExpression.getParameters()) {
      addTypeDescriptor(parameter.getTypeDescriptor(), ImportCategory.LAZY);
    }
  }

  @Override
  public void exitType(Type type) {
    addTypeDescriptor(type.getDeclaration().getUnsafeTypeDescriptor(), ImportCategory.SELF);

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
  public void exitJsDocAnnotatedExpression(JsDocAnnotatedExpression jsDocAnnotatedExpression) {
    addTypeDescriptor(jsDocAnnotatedExpression.getTypeDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitMethod(Method method) {
    TypeDescriptor returnTypeDescriptor = method.getDescriptor().getReturnTypeDescriptor();
    if (!returnTypeDescriptor.isPrimitive()
        || TypeDescriptors.isPrimitiveLong(returnTypeDescriptor)) {
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
    if (TypeDescriptors.isPrimitiveLong(numberLiteral.getTypeDescriptor())) {
      addTypeDescriptor(BootstrapType.NATIVE_LONG.getDescriptor(), ImportCategory.EAGER);
    }
  }

  @Override
  public void exitTypeReference(TypeReference typeReference) {
    addTypeDescriptor(typeReference.getReferencedTypeDescriptor(), ImportCategory.LAZY);
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
    // Type variables can't be depended upon.
    if (typeDescriptor.isTypeVariable()
        || typeDescriptor.isWildCardOrCapture()
        || typeDescriptor.isIntersection()) {
      return;
    }

    // Special case expand a dependency on the 'long' primitive into a dependency on both the 'long'
    // primitive and the native JS 'Long' emulation class.
    if (TypeDescriptors.isPrimitiveLong(typeDescriptor)) {
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
    if (typeDescriptor.hasTypeArguments()) {
      for (TypeDescriptor typeArgumentDescriptor : typeDescriptor.getTypeArgumentDescriptors()) {
        // But the type argument imports do not need to be eager since they are not acting here as
        // super type or super interface.
        addTypeDescriptor(typeArgumentDescriptor, ImportCategory.LAZY);
      }
    }

    mayAddTypeDescriptorsIntroducedByJsFunction(typeDescriptor);
    addRawTypeDescriptor(importCategory, typeDescriptor.getRawTypeDescriptor());
  }

  private Map<ImportCategory, Set<Import>> doGatherImports(Type type) {
    TimingCollector timingCollector = TimingCollector.get();
    timingCollector.startSubSample("Add default Classes");

    if (type.isJsOverlayImplementation()) {
      // The synthesized JsOverlayImpl type should import the native type eagerly.
      addTypeDescriptor(type.getNativeTypeDescriptor(), ImportCategory.EAGER);
    }
    // Util class implements some utility functions and does not depend on any other class, always
    // import it eagerly.
    addTypeDescriptor(BootstrapType.NATIVE_UTIL.getDescriptor(), ImportCategory.EAGER);

    // Collect type references.
    timingCollector.startSample("Collect type references");
    type.accept(this);

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
    // Creates an alias for the current type, last, to make sure that its name dodges externs
    // when necessary.
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
    // TODO: We should also include TypeVariables on name recording.
    for (TypeDescriptor typeDescriptor : typeDescriptors) {
      if (typeDescriptor.isExtern()) {
        // Reserve the top qualifier for externs to avoid clashes. Externs are qualified names such
        // as window.String, for that scenario only the top level qualifier "window" needs to be
        // avoided.
        String topLevelExtern = typeDescriptor.getQualifiedJsName().split("\\.")[0];
        localNameUses.add(topLevelExtern);
      } else {
        localNameUses.add(computeShortAliasName(typeDescriptor));
      }
    }
  }

  private Set<Import> toImports(Set<TypeDescriptor> typeDescriptors) {
    Set<Import> imports = new LinkedHashSet<>();
    for (TypeDescriptor typeDescriptor : typeDescriptors) {
      Preconditions.checkState(!typeDescriptor.isTypeVariable());
      Preconditions.checkState(typeDescriptor.isNative() || !typeDescriptor.hasTypeArguments());
      imports.add(new Import(computeAlias(typeDescriptor), typeDescriptor));
    }
    return imports;
  }

  private String computeAlias(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isExtern()) {
      return typeDescriptor.getQualifiedJsName();
    }

    String shortAliasName = computeShortAliasName(typeDescriptor);
    boolean unique = localNameUses.count(shortAliasName) == 1;
    return unique && JsProtectedNames.isLegalName(shortAliasName)
        ? shortAliasName
        : computeLongAliasName(typeDescriptor);
  }

  private static String computeLongAliasName(TypeDescriptor typeDescriptor) {
    return typeDescriptor.getQualifiedSourceName().replaceAll("_", "__").replaceAll("\\.", "_");
  }

  private static String computeShortAliasName(TypeDescriptor typeDescriptor) {
    // Add "$" prefix for bootstrap types and primitive types.
    if (BootstrapType.typeDescriptors.contains(TypeDescriptors.toNullable(typeDescriptor))
        || typeDescriptor.isPrimitive()) {
      return "$" + typeDescriptor.getSimpleSourceName();
    }
    return typeDescriptor.getSimpleSourceName();
  }
}
