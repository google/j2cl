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
import com.google.j2cl.ast.JavaScriptConstructorReference;
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

  /** Describes the category of an import. */
  public enum ImportCategory {
    /** Used in load-time during initial load of the classes. */
    LOADTIME,
    /** Used in run-time during method execution. */
    RUNTIME,
    /** Used for JsDoc purposes and in some cases for circumventing AJD pruning. */
    JSDOC,
    /** Not emitted and only exists to ensure that the name is preserved. */
    EXTERN,
    /** Not emitted and only exists to ensure that an alias is created for the current type. */
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
    addTypeDescriptor(BootstrapType.ASSERTS.getDescriptor(), ImportCategory.RUNTIME);
  }

  @Override
  public void exitField(Field field) {
    maybeAddNativeReference(field);
    // A static long field needs to be initialized a load time which is done by code generator.
    if (TypeDescriptors.isPrimitiveLong(field.getDescriptor().getTypeDescriptor())) {
      addTypeDescriptor(BootstrapType.NATIVE_LONG.getDescriptor(), ImportCategory.LOADTIME);
    }
    collectForJsDoc(field.getDescriptor().getTypeDescriptor());
  }

  @Override
  public void exitFunctionExpression(FunctionExpression functionExpression) {
    for (Variable parameter : functionExpression.getParameters()) {
      collectForJsDoc(parameter.getTypeDescriptor());
    }
  }

  @Override
  public void exitType(Type type) {
    addTypeDescriptor(type.getTypeDescriptor(), ImportCategory.SELF);

    if (type.isJsOverlayImplementation() && type.getNativeTypeDescriptor().isNative()) {
      // The synthesized JsOverlayImpl type should import the native type eagerly for $isInstance.
      // Also requiring native type makes sure the native type is not pruned by AJD.
      addTypeDescriptor(type.getNativeTypeDescriptor(), ImportCategory.LOADTIME);
    }

    // Super type and super interface imports are needed eagerly because they are used during the
    // declaration phase of JS execution. All other imports are lazy.
    if (type.getSuperTypeDescriptor() != null) {
      collectForJsDoc(type.getSuperTypeDescriptor());
      addTypeDescriptor(type.getSuperTypeDescriptor(), ImportCategory.LOADTIME);
    }
    for (TypeDescriptor superInterfaceTypeDescriptor : type.getSuperInterfaceTypeDescriptors()) {
      collectForJsDoc(superInterfaceTypeDescriptor);
      // JsFunction super interface reference is replaced with generic one by the code generator.
      if (superInterfaceTypeDescriptor.isJsFunctionInterface()) {
        superInterfaceTypeDescriptor = BootstrapType.JAVA_SCRIPT_FUNCTION.getDescriptor();
      }
      addTypeDescriptor(superInterfaceTypeDescriptor, ImportCategory.LOADTIME);
    }

    // Here we add an extra dependency on the outer namespace if declareLegacyNamespace is
    // enabled.  This forces the outer namespaces to be declared first before the inner namespace
    // which avoids errors in the JsCompiler. Note this interacts poorly with Outer classes that
    // implement/extend Inner types.
    if (declareLegacyNamespace
        && AstUtils.canBeRequiredFromJs(type.getDeclaration())
        && type.getDeclaration().getEnclosingTypeDeclaration() != null) {
      addTypeDescriptor(
          type.getDeclaration().getEnclosingTypeDeclaration().getUnsafeTypeDescriptor(),
          ImportCategory.LOADTIME);
    }
  }

  @Override
  public void exitJsDocAnnotatedExpression(JsDocAnnotatedExpression jsDocAnnotatedExpression) {
    collectForJsDoc(jsDocAnnotatedExpression.getTypeDescriptor());
  }

  @Override
  public void exitVariableDeclarationFragment(
      VariableDeclarationFragment variableDeclarationFragment) {
    if (variableDeclarationFragment.needsTypeDeclaration()) {
      Variable variable = variableDeclarationFragment.getVariable();
      collectForJsDoc(variable.getTypeDescriptor());
    }
  }

  @Override
  public void exitMethod(Method method) {
    maybeAddNativeReference(method);

    collectForJsDoc(method.getDescriptor().getReturnTypeDescriptor());

    for (Variable parameter : method.getParameters()) {
      collectForJsDoc(parameter.getTypeDescriptor());
    }
  }

  private void maybeAddNativeReference(Member member) {
    MemberDescriptor memberDescriptor = member.getDescriptor();
    if (memberDescriptor.isNative()
        && memberDescriptor.isStatic()
        && memberDescriptor.hasJsNamespace()
        && !memberDescriptor.isExtern()) {
      addTypeDescriptor(
          AstUtils.getNamespaceAsTypeDescriptor(memberDescriptor), ImportCategory.RUNTIME);
    }
  }

  @Override
  public void exitMethodCall(MethodCall methodCall) {
    if (methodCall.isStaticDispatch()) {
      addTypeDescriptor(
          methodCall.getTarget().getEnclosingTypeDescriptor(), ImportCategory.RUNTIME);
    }
  }

  @Override
  public void exitNewInstance(NewInstance newInstance) {
    addTypeDescriptor(newInstance.getTarget().getEnclosingTypeDescriptor(), ImportCategory.RUNTIME);
  }

  @Override
  public void exitNumberLiteral(NumberLiteral numberLiteral) {
    // Long number literal are transformed by expression transpiler to use NATIVE_LONG.
    if (TypeDescriptors.isPrimitiveLong(numberLiteral.getTypeDescriptor())) {
      addTypeDescriptor(BootstrapType.NATIVE_LONG.getDescriptor(), ImportCategory.LOADTIME);
    }
  }

  @SuppressWarnings("ReferenceEquality")
  @Override
  public void exitJavaScriptConstructorReference(
      JavaScriptConstructorReference constructorReference) {
    TypeDescriptor referencedTypeDescriptor = constructorReference.getReferencedTypeDescriptor();
    if (referencedTypeDescriptor == TypeDescriptors.get().globalNamespace) {
      // We don't need to record global since it doesn't have a name but we still want the rest of
      // the extern since they have a name that we should record and preserve.
      return;
    }
    addTypeDescriptor(referencedTypeDescriptor, ImportCategory.RUNTIME);
  }

  private void collectForJsDoc(TypeDescriptor typeDescriptor) {
    // Overlay classes may be referred directly from other compilation units that are compiled
    // separately. In order for these classes to be preserved and not pruned by AJD, any user of the
    // original class should have a dependency on the overlay class.
    if (typeDescriptor.hasOverlayImplementationType()) {
      collectForJsDoc(typeDescriptor.getTypeDeclaration().getOverlayImplementationTypeDescriptor());
    }

    // JsDoc for {@code long} uses NATIVE_LONG.
    if (TypeDescriptors.isPrimitiveLong(typeDescriptor)) {
      collectForJsDoc(BootstrapType.NATIVE_LONG.getDescriptor());
      return;
    }

    // JsDoc for primitives uses Closure builtins.
    if (typeDescriptor.isPrimitive()) {
      return;
    }

    // JsDoc for arrays uses Closure builtin but template part requires the leaf type.
    if (typeDescriptor.isArray()) {
      collectForJsDoc(typeDescriptor.getLeafTypeDescriptor());
      return;
    }

    if (typeDescriptor.isJsFunctionInterface() || typeDescriptor.isJsFunctionImplementation()) {
      collectTypeDescriptorsIntroducedByJsFunction(typeDescriptor);
      return;
    }

    if (typeDescriptor.isTypeVariable() || typeDescriptor.isWildCardOrCapture()) {
      collectTypeDescriptorsIntroducedByTypeBounds(typeDescriptor);
      return;
    }

    if (typeDescriptor.hasTypeArguments()) {
      for (TypeDescriptor typeArgumentDescriptor : typeDescriptor.getTypeArgumentDescriptors()) {
        collectForJsDoc(typeArgumentDescriptor);
      }
    }

    addTypeDescriptor(typeDescriptor, ImportCategory.JSDOC);
  }

  /**
   * JsFunction type is annotated as function(Foo):Bar, we need to import the parameter types and
   * return type.
   */
  private void collectTypeDescriptorsIntroducedByJsFunction(TypeDescriptor typeDescriptor) {
    MethodDescriptor jsFunctionMethodDescriptor =
        typeDescriptor.getConcreteJsFunctionMethodDescriptor();
    for (TypeDescriptor parameterTypeDescriptor :
        jsFunctionMethodDescriptor.getParameterTypeDescriptors()) {
      collectForJsDoc(parameterTypeDescriptor);
    }
    collectForJsDoc(jsFunctionMethodDescriptor.getReturnTypeDescriptor());
  }

  /**
   * Type bounds may be implicitly referred from other compilation units that are compiled
   * separately due to synthesized erasure casts. In order for these classes to be preserved and not
   * pruned by AJD, we should create a dependency to the bound.
   */
  private void collectTypeDescriptorsIntroducedByTypeBounds(TypeDescriptor typeDescriptor) {
    TypeDescriptor boundTypeDescriptor = typeDescriptor.getBoundTypeDescriptor();

    if (boundTypeDescriptor == TypeDescriptors.get().javaLangObject) {
      // Effectively unbounded and will not result in erasure casts.
      return;
    }

    // Avoid recursing into bounds type arguments, as these are not needed and they might introduce
    // an infinite loop (e.g. class Enum<T extends Enum<T>>).
    if (boundTypeDescriptor.hasTypeArguments()) {
      boundTypeDescriptor = boundTypeDescriptor.getRawTypeDescriptor();
    }

    if (boundTypeDescriptor.isIntersection()) {
      // TODO(68033041): add missing case to ajd_prune_type_variable_bounds and handle this.
    } else {
      collectForJsDoc(boundTypeDescriptor);
    }
  }

  private void addTypeDescriptor(TypeDescriptor typeDescriptor, ImportCategory importCategory) {
    checkArgument(
        !typeDescriptor.isJsFunctionInterface()
            && !typeDescriptor.isArray()
            && !typeDescriptor.isUnion()
            && !typeDescriptor.isIntersection()
            && !typeDescriptor.isWildCardOrCapture()
            && !typeDescriptor.isTypeVariable());
    checkArgument(!typeDescriptor.getQualifiedJsName().isEmpty());

    TypeDescriptor rawTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
    if (rawTypeDescriptor.isExtern()) {
      importCategory = ImportCategory.EXTERN;
    }
    typeDescriptorsByCategory.put(importCategory, rawTypeDescriptor);
  }

  private Multimap<ImportCategory, Import> doGatherImports(Type type) {
    TimingCollector timingCollector = TimingCollector.get();
    timingCollector.startSubSample("Add default Classes");

    // Util class implements some utility functions and does not depend on any other class, always
    // import it eagerly.
    addTypeDescriptor(BootstrapType.NATIVE_UTIL.getDescriptor(), ImportCategory.LOADTIME);

    // Collect type references.
    timingCollector.startSample("Collect type references");
    type.accept(this);

    Preconditions.checkState(typeDescriptorsByCategory.get(ImportCategory.SELF).size() == 1);

    timingCollector.startSample("Remove duplicate references");
    typeDescriptorsByCategory
        .get(ImportCategory.RUNTIME)
        .removeAll(typeDescriptorsByCategory.get(ImportCategory.LOADTIME));
    typeDescriptorsByCategory
        .get(ImportCategory.RUNTIME)
        .removeAll(typeDescriptorsByCategory.get(ImportCategory.SELF));
    typeDescriptorsByCategory
        .get(ImportCategory.LOADTIME)
        .removeAll(typeDescriptorsByCategory.get(ImportCategory.SELF));

    typeDescriptorsByCategory
        .get(ImportCategory.JSDOC)
        .removeAll(typeDescriptorsByCategory.get(ImportCategory.LOADTIME));
    typeDescriptorsByCategory
        .get(ImportCategory.JSDOC)
        .removeAll(typeDescriptorsByCategory.get(ImportCategory.RUNTIME));
    typeDescriptorsByCategory
        .get(ImportCategory.JSDOC)
        .removeAll(typeDescriptorsByCategory.get(ImportCategory.SELF));

    timingCollector.startSample("Record Local Name Uses");
    recordLocalNameUses(typeDescriptorsByCategory.get(ImportCategory.SELF));
    recordLocalNameUses(typeDescriptorsByCategory.get(ImportCategory.RUNTIME));
    recordLocalNameUses(typeDescriptorsByCategory.get(ImportCategory.LOADTIME));
    recordLocalNameUses(typeDescriptorsByCategory.get(ImportCategory.JSDOC));
    recordLocalNameUses(typeDescriptorsByCategory.get(ImportCategory.EXTERN));

    timingCollector.startSample("Convert to imports");
    Multimap<ImportCategory, Import> importsByCategory = LinkedHashMultimap.create();
    importsByCategory.putAll(
        ImportCategory.RUNTIME, toImports(typeDescriptorsByCategory.get(ImportCategory.RUNTIME)));
    importsByCategory.putAll(
        ImportCategory.LOADTIME, toImports(typeDescriptorsByCategory.get(ImportCategory.LOADTIME)));
    importsByCategory.putAll(
        ImportCategory.JSDOC, toImports(typeDescriptorsByCategory.get(ImportCategory.JSDOC)));
    importsByCategory.putAll(
        ImportCategory.EXTERN, toImports(typeDescriptorsByCategory.get(ImportCategory.EXTERN)));
    // Creates an alias for the current type, last, to make sure that its name dodges externs
    // when necessary.
    importsByCategory.putAll(
        ImportCategory.SELF, toImports(typeDescriptorsByCategory.get(ImportCategory.SELF)));
    timingCollector.endSubSample();
    return importsByCategory;
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
      Preconditions.checkState(!typeDescriptor.hasTypeArguments());
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
