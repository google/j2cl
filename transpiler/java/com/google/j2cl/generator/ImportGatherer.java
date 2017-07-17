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
package com.google.j2cl.generator;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.JsDocAnnotatedExpression;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.MemberDescriptor;
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
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.common.TimingCollector;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Traverses a Type, gathers imports for all things it references and creates non colliding local
 * aliases for each import.
 */
class ImportGatherer extends AbstractVisitor {

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

  public static Multimap<ImportCategory, Import> gatherImports(
      Type type, boolean declareLegacyNamespace) {
    TimingCollector.get().startSubSample("Import Gathering Visitor");

    Multimap<ImportCategory, Import> importsByCategory =
        new ImportGatherer(declareLegacyNamespace).doGatherImports(type);
    TimingCollector.get().endSubSample();
    return importsByCategory;
  }

  private final Multiset<String> localNameUses = HashMultiset.create();

  private final SetMultimap<ImportCategory, TypeDescriptor> typeDescriptorsByCategory =
      LinkedHashMultimap.create();

  private final boolean declareLegacyNamespace;

  private ImportGatherer(boolean declareLegacyNamespace) {
    this.declareLegacyNamespace = declareLegacyNamespace;
  }

  @Override
  public void exitAssertStatement(AssertStatement assertStatement) {
    addTypeDescriptor(BootstrapType.ASSERTS.getDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitField(Field field) {
    maybeAddNativeReference(field);
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

    // Here we add an extra dependency on the outter namespace if declareLegacyNamespace is
    // enabled.  This forces the outter namespaces to be declared first before the inner namespace
    // which avoids errors in the JsCompiler. Note this interacts poorly with Outer classes that
    // implement/extend Inner types.
    if (declareLegacyNamespace
        && AstUtils.canBeRequiredFromJs(type.getDeclaration())
        && type.getDeclaration().getEnclosingTypeDeclaration() != null) {
      addTypeDescriptor(
          type.getDeclaration().getEnclosingTypeDeclaration().getUnsafeTypeDescriptor(),
          ImportCategory.EAGER);
    }
  }

  @Override
  public void exitJsDocAnnotatedExpression(JsDocAnnotatedExpression jsDocAnnotatedExpression) {
    addTypeDescriptor(jsDocAnnotatedExpression.getTypeDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitVariableDeclarationFragment(
      VariableDeclarationFragment variableDeclarationFragment) {
    if (variableDeclarationFragment.needsTypeDeclaration()) {
      Variable variable = variableDeclarationFragment.getVariable();
      addTypeDescriptor(variable.getTypeDescriptor(), ImportCategory.LAZY);
    }
  }

  @Override
  public void exitMethod(Method method) {
    maybeAddNativeReference(method);
    TypeDescriptor returnTypeDescriptor = method.getDescriptor().getReturnTypeDescriptor();
    if (!returnTypeDescriptor.isPrimitive()
        || TypeDescriptors.isPrimitiveLong(returnTypeDescriptor)) {
      addTypeDescriptor(returnTypeDescriptor, ImportCategory.LAZY);
    }

    for (Variable parameter : method.getParameters()) {
      addTypeDescriptor(parameter.getTypeDescriptor(), ImportCategory.LAZY);
    }
  }

  private void maybeAddNativeReference(Member member) {
    MemberDescriptor memberDescriptor = member.getDescriptor();
    if (memberDescriptor.isNative()
        && memberDescriptor.isStatic()
        && memberDescriptor.hasJsNamespace()
        && !memberDescriptor.isExtern()) {
      addTypeDescriptor(
          AstUtils.getNamespaceAsTypeDescriptor(memberDescriptor), ImportCategory.LAZY);
    }
  }

  @Override
  public void exitMethodCall(MethodCall methodCall) {
    if (methodCall.isStaticDispatch()) {
      addTypeDescriptor(methodCall.getTarget().getEnclosingTypeDescriptor(), ImportCategory.LAZY);
    }
  }

  @Override
  public void exitNewInstance(NewInstance newInstance) {
    addTypeDescriptor(newInstance.getTarget().getEnclosingTypeDescriptor(), ImportCategory.LAZY);
  }

  @Override
  public void exitNumberLiteral(NumberLiteral numberLiteral) {
    if (TypeDescriptors.isPrimitiveLong(numberLiteral.getTypeDescriptor())) {
      addTypeDescriptor(BootstrapType.NATIVE_LONG.getDescriptor(), ImportCategory.EAGER);
    }
  }

  @SuppressWarnings("ReferenceEquality")
  @Override
  public void exitTypeReference(TypeReference typeReference) {
    TypeDescriptor referencedTypeDescriptor = typeReference.getReferencedTypeDescriptor();
    if (referencedTypeDescriptor == TypeDescriptors.get().globalNamespace) {
      return;
    }
    addTypeDescriptor(referencedTypeDescriptor, ImportCategory.LAZY);
  }

  private void addTypeDescriptor(TypeDescriptor typeDescriptor, ImportCategory importCategory) {
    // Type variables can't be depended upon.
    if (typeDescriptor.isTypeVariable() || typeDescriptor.isWildCardOrCapture()) {
      TypeDescriptor boundTypeDescriptor = typeDescriptor.getBoundTypeDescriptor();
      if (boundTypeDescriptor != null) {
        // Add type descriptor appearing as bounds of type variables. These are needed because
        // j2cl code in dependent libraries might synthesize erasure casts that refer to these
        // types.
        // Avoid recursing into bounds type arguments, as these are not needed and they might
        // introduce an infinite loop (e.g. class Enum<T extends Enum<T>>).
        addTypeDescriptor(
            boundTypeDescriptor.hasTypeArguments()
                ? boundTypeDescriptor.getRawTypeDescriptor()
                : boundTypeDescriptor,
            ImportCategory.LAZY);
      }
      return;
    }

    if (typeDescriptor.isIntersection()) {
      return;
    }

    checkArgument(!typeDescriptor.getQualifiedJsName().isEmpty());
    // Special case expand a dependency on the 'long' primitive into a dependency on both the 'long'
    // primitive and the native JS 'Long' emulation class.
    if (TypeDescriptors.isPrimitiveLong(typeDescriptor)) {
      typeDescriptorsByCategory.put(
          ImportCategory.EAGER, BootstrapType.NATIVE_LONG.getDescriptor());
      typeDescriptorsByCategory.put(importCategory, TypeDescriptors.get().primitiveLong);
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
    mayAddOverlayImplementationTypeDescriptor(typeDescriptor);

    TypeDescriptor rawTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
    if (rawTypeDescriptor.isExtern()) {
      importCategory = ImportCategory.EXTERN;
    }

    typeDescriptorsByCategory.put(importCategory, rawTypeDescriptor);
  }

  private void mayAddOverlayImplementationTypeDescriptor(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.hasOverlayImplementationType()) {
      addTypeDescriptor(
          typeDescriptor.getTypeDeclaration().getOverlayImplementationTypeDescriptor(),
          ImportCategory.LAZY);
    }
  }

  private Multimap<ImportCategory, Import> doGatherImports(Type type) {
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
    recordLocalNameUses(typeDescriptorsByCategory.get(ImportCategory.SELF));
    recordLocalNameUses(typeDescriptorsByCategory.get(ImportCategory.LAZY));
    recordLocalNameUses(typeDescriptorsByCategory.get(ImportCategory.EAGER));
    recordLocalNameUses(typeDescriptorsByCategory.get(ImportCategory.EXTERN));

    timingCollector.startSample("Convert to imports");
    Multimap<ImportCategory, Import> importsByCategory = LinkedHashMultimap.create();
    importsByCategory.putAll(
        ImportCategory.LAZY, toImports(typeDescriptorsByCategory.get(ImportCategory.LAZY)));
    importsByCategory.putAll(
        ImportCategory.EAGER, toImports(typeDescriptorsByCategory.get(ImportCategory.EAGER)));
    importsByCategory.putAll(
        ImportCategory.EXTERN, toImports(typeDescriptorsByCategory.get(ImportCategory.EXTERN)));
    // Creates an alias for the current type, last, to make sure that its name dodges externs
    // when necessary.
    importsByCategory.putAll(
        ImportCategory.SELF, toImports(typeDescriptorsByCategory.get(ImportCategory.SELF)));
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
    // TODO(goktug): We should also include TypeVariables on name recording.
    for (TypeDescriptor typeDescriptor : typeDescriptors) {
      if (typeDescriptor.isExtern()) {
        // Reserve the top qualifier for externs to avoid clashes. Externs are qualified names such
        // as window.String, for that scenario only the top level qualifier "window" needs to be
        // avoided.
        String topLevelExtern = typeDescriptor.getQualifiedJsName().split("\\.")[0];
        localNameUses.add(topLevelExtern);
      } else {
        localNameUses.add(typeDescriptor.getShortAliasName());
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

    String shortAliasName = typeDescriptor.getShortAliasName();
    boolean unique = localNameUses.count(shortAliasName) == 1;
    return unique && JsProtectedNames.isLegalName(shortAliasName)
        ? shortAliasName
        : typeDescriptor.getLongAliasName();
  }
}
