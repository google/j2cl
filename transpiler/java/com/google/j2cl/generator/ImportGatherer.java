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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.IntersectionTypeDescriptor;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.JsDocFieldDeclaration;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.MemberDescriptor;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.TypeVariable;
import com.google.j2cl.ast.UnionTypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.common.TimingCollector;
import java.util.HashSet;
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

  private final Set<TypeDescriptor> collectedForJsDoc = new HashSet<>();

  private final SetMultimap<ImportCategory, TypeDeclaration> typeDeclarationByCategory =
      LinkedHashMultimap.create();

  private final boolean declareLegacyNamespace;

  private ImportGatherer(boolean declareLegacyNamespace) {
    this.declareLegacyNamespace = declareLegacyNamespace;
  }

  @Override
  public void exitAssertStatement(AssertStatement assertStatement) {
    addTypeDeclaration(BootstrapType.ASSERTS.getDeclaration(), ImportCategory.RUNTIME);
  }

  @Override
  public void exitField(Field field) {
    maybeAddNativeReference(field);
    // A static long field needs to be initialized a load time which is done by code generator.
    if (TypeDescriptors.isPrimitiveLong(field.getDescriptor().getTypeDescriptor())) {
      addTypeDeclaration(BootstrapType.NATIVE_LONG.getDeclaration(), ImportCategory.LOADTIME);
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
    addTypeDeclaration(type.getDeclaration(), ImportCategory.SELF);

    if (type.isJsOverlayImplementation() && type.getNativeTypeDescriptor().isNative()) {
      // The synthesized JsOverlayImpl type should import the native type eagerly for $isInstance.
      // Also requiring native type makes sure the native type is not pruned by AJD.
      addTypeDeclaration(
          type.getNativeTypeDescriptor().getTypeDeclaration(), ImportCategory.LOADTIME);
    }

    // Super type and super interface imports are needed eagerly because they are used during the
    // declaration phase of JS execution. All other imports are lazy.
    if (type.getSuperTypeDescriptor() != null) {
      collectForJsDoc(type.getSuperTypeDescriptor());
      addTypeDeclaration(
          type.getSuperTypeDescriptor().getTypeDeclaration(), ImportCategory.LOADTIME);
    }
    for (DeclaredTypeDescriptor superInterfaceTypeDescriptor :
        type.getSuperInterfaceTypeDescriptors()) {
      collectForJsDoc(superInterfaceTypeDescriptor);
      // JsFunction super interface reference is replaced with generic one by the code generator.
      if (superInterfaceTypeDescriptor.isJsFunctionInterface()) {
        superInterfaceTypeDescriptor = BootstrapType.JAVA_SCRIPT_FUNCTION.getDescriptor();
      }
      addTypeDeclaration(
          superInterfaceTypeDescriptor.getTypeDeclaration(), ImportCategory.LOADTIME);
    }

    // Here we add an extra dependency on the outer namespace if declareLegacyNamespace is
    // enabled.  This forces the outer namespaces to be declared first before the inner namespace
    // which avoids errors in the JsCompiler. Note this interacts poorly with Outer classes that
    // implement/extend Inner types.
    TypeDeclaration enclosingTypeDeclaration = type.getDeclaration().getEnclosingTypeDeclaration();
    if (declareLegacyNamespace
        && AstUtils.canBeRequiredFromJs(type.getDeclaration())
        && enclosingTypeDeclaration != null) {
      addTypeDeclaration(enclosingTypeDeclaration, ImportCategory.LOADTIME);
    }
  }

  @Override
  public void exitJsDocCastExpression(JsDocCastExpression jsDocCastExpression) {
    collectForJsDoc(jsDocCastExpression.getTypeDescriptor());
  }

  @Override
  public void exitJsDocFieldDeclaration(JsDocFieldDeclaration jsDocFieldDeclaration) {
    collectForJsDoc(jsDocFieldDeclaration.getTypeDescriptor());
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
      addTypeDeclaration(
          AstUtils.getNamespaceAsTypeDescriptor(memberDescriptor).getTypeDeclaration(),
          ImportCategory.RUNTIME);
    }
  }

  @Override
  public void exitMethodCall(MethodCall methodCall) {
    if (methodCall.isStaticDispatch()) {
      addTypeDeclaration(
          methodCall.getTarget().getEnclosingTypeDescriptor().getTypeDeclaration(),
          ImportCategory.RUNTIME);
    }
  }

  @Override
  public void exitNewInstance(NewInstance newInstance) {
    addTypeDeclaration(
        newInstance.getTarget().getEnclosingTypeDescriptor().getTypeDeclaration(),
        ImportCategory.RUNTIME);
  }

  @Override
  public void exitNumberLiteral(NumberLiteral numberLiteral) {
    // Long number literal are transformed by expression transpiler to use NATIVE_LONG.
    if (TypeDescriptors.isPrimitiveLong(numberLiteral.getTypeDescriptor())) {
      addTypeDeclaration(BootstrapType.NATIVE_LONG.getDeclaration(), ImportCategory.LOADTIME);
    }
  }

  @SuppressWarnings("ReferenceEquality")
  @Override
  public void exitJavaScriptConstructorReference(
      JavaScriptConstructorReference constructorReference) {
    TypeDeclaration referencedTypeDeclaration = constructorReference.getReferencedTypeDeclaration();
    if (referencedTypeDeclaration == TypeDescriptors.get().globalNamespace.getTypeDeclaration()) {
      // We don't need to record global since it doesn't have a name but we still want the rest of
      // the extern since they have a name that we should record and preserve.
      return;
    }
    addTypeDeclaration(referencedTypeDeclaration, ImportCategory.RUNTIME);
  }

  private void collectForJsDoc(TypeDescriptor typeDescriptor) {
    // Avoid the recursion that might arise from type variable declarations,
    // (e.g. class Enum<T extends Enum<T>>).
    if (!collectedForJsDoc.add(typeDescriptor)) {
      return;
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
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      collectForJsDoc(arrayTypeDescriptor.getLeafTypeDescriptor());
      return;
    }

    if (typeDescriptor.isUnion()) {
      UnionTypeDescriptor unionTypeDescriptor = (UnionTypeDescriptor) typeDescriptor;
      unionTypeDescriptor.getUnionTypeDescriptors().forEach(this::collectForJsDoc);
      return;
    }

    if (typeDescriptor instanceof TypeVariable) {
      collectTypeDescriptorsIntroducedByTypeBounds((TypeVariable) typeDescriptor);
      return;
    }

    if (typeDescriptor.isIntersection()) {
      IntersectionTypeDescriptor intersectionTypeDescriptor =
          (IntersectionTypeDescriptor) typeDescriptor;
      intersectionTypeDescriptor.getIntersectionTypeDescriptors().forEach(this::collectForJsDoc);
      return;
    }

    DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;

    // Overlay classes may be referred directly from other compilation units that are compiled
    // separately. In order for these classes to be preserved and not pruned by AJD, any user of the
    // original class should have a dependency on the overlay class.
    TypeDeclaration typeDeclaration = declaredTypeDescriptor.getTypeDeclaration();
    if (typeDeclaration.hasOverlayImplementationType()) {
      collectForJsDoc(typeDeclaration.getOverlayImplementationTypeDescriptor());
    }

    if (typeDescriptor.isJsFunctionInterface() || typeDescriptor.isJsFunctionImplementation()) {
      collectTypeDescriptorsIntroducedByJsFunction(declaredTypeDescriptor);
      return;
    }

    declaredTypeDescriptor.getTypeArgumentDescriptors().forEach(this::collectForJsDoc);

    addTypeDeclaration(declaredTypeDescriptor.getTypeDeclaration(), ImportCategory.JSDOC);
  }

  /**
   * JsFunction type is annotated as function(Foo):Bar, we need to import the parameter types and
   * return type.
   */
  private void collectTypeDescriptorsIntroducedByJsFunction(DeclaredTypeDescriptor typeDescriptor) {
    MethodDescriptor jsFunctionMethodDescriptor = typeDescriptor.getJsFunctionMethodDescriptor();
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
  private void collectTypeDescriptorsIntroducedByTypeBounds(TypeVariable typeVariable) {
    TypeDescriptor boundTypeDescriptor = typeVariable.getBoundTypeDescriptor();

    if (TypeDescriptors.isJavaLangObject(boundTypeDescriptor)) {
      // Effectively unbounded and will not result in erasure casts.
      return;
    }

    collectForJsDoc(boundTypeDescriptor);
  }

  private void addTypeDeclaration(TypeDeclaration typeDeclaration, ImportCategory importCategory) {
    checkArgument(!typeDeclaration.isJsFunctionInterface());
    checkArgument(!typeDeclaration.getQualifiedJsName().isEmpty());

    if (typeDeclaration.isExtern()) {
      importCategory = ImportCategory.EXTERN;
    }
    typeDeclarationByCategory.put(importCategory, typeDeclaration);
  }

  private Multimap<ImportCategory, Import> doGatherImports(Type type) {
    TimingCollector timingCollector = TimingCollector.get();
    timingCollector.startSubSample("Add default Classes");

    // Util class implements some utility functions and does not depend on any other class, always
    // import it eagerly.
    addTypeDeclaration(BootstrapType.NATIVE_UTIL.getDeclaration(), ImportCategory.LOADTIME);

    // Collect type references.
    timingCollector.startSample("Collect type references");
    type.accept(this);

    checkState(typeDeclarationByCategory.get(ImportCategory.SELF).size() == 1);

    timingCollector.startSample("Remove duplicate references");
    typeDeclarationByCategory
        .get(ImportCategory.RUNTIME)
        .removeAll(typeDeclarationByCategory.get(ImportCategory.LOADTIME));
    typeDeclarationByCategory
        .get(ImportCategory.RUNTIME)
        .removeAll(typeDeclarationByCategory.get(ImportCategory.SELF));
    typeDeclarationByCategory
        .get(ImportCategory.LOADTIME)
        .removeAll(typeDeclarationByCategory.get(ImportCategory.SELF));

    typeDeclarationByCategory
        .get(ImportCategory.JSDOC)
        .removeAll(typeDeclarationByCategory.get(ImportCategory.LOADTIME));
    typeDeclarationByCategory
        .get(ImportCategory.JSDOC)
        .removeAll(typeDeclarationByCategory.get(ImportCategory.RUNTIME));
    typeDeclarationByCategory
        .get(ImportCategory.JSDOC)
        .removeAll(typeDeclarationByCategory.get(ImportCategory.SELF));

    timingCollector.startSample("Record Local Name Uses");
    recordLocalNameUses(typeDeclarationByCategory.get(ImportCategory.SELF));
    recordLocalNameUses(typeDeclarationByCategory.get(ImportCategory.RUNTIME));
    recordLocalNameUses(typeDeclarationByCategory.get(ImportCategory.LOADTIME));
    recordLocalNameUses(typeDeclarationByCategory.get(ImportCategory.JSDOC));
    recordLocalNameUses(typeDeclarationByCategory.get(ImportCategory.EXTERN));

    timingCollector.startSample("Convert to imports");
    Multimap<ImportCategory, Import> importsByCategory = LinkedHashMultimap.create();
    importsByCategory.putAll(
        ImportCategory.RUNTIME, toImports(typeDeclarationByCategory.get(ImportCategory.RUNTIME)));
    importsByCategory.putAll(
        ImportCategory.LOADTIME, toImports(typeDeclarationByCategory.get(ImportCategory.LOADTIME)));
    importsByCategory.putAll(
        ImportCategory.JSDOC, toImports(typeDeclarationByCategory.get(ImportCategory.JSDOC)));
    importsByCategory.putAll(
        ImportCategory.EXTERN, toImports(typeDeclarationByCategory.get(ImportCategory.EXTERN)));
    // Creates an alias for the current type, last, to make sure that its name dodges externs
    // when necessary.
    importsByCategory.putAll(
        ImportCategory.SELF, toImports(typeDeclarationByCategory.get(ImportCategory.SELF)));
    timingCollector.endSubSample();
    return importsByCategory;
  }

  private void recordLocalNameUses(Set<TypeDeclaration> typeDeclarations) {
    // TODO(goktug): We should also include TypeVariables on name recording.
    for (TypeDeclaration typeDeclaration : typeDeclarations) {
      if (typeDeclaration.isExtern()) {
        // Reserve the top qualifier for externs to avoid clashes. Externs are qualified names such
        // as window.String, for that scenario only the top level qualifier "window" needs to be
        // avoided.
        String topLevelExtern = typeDeclaration.getQualifiedJsName().split("\\.")[0];
        localNameUses.add(topLevelExtern);
      } else {
        localNameUses.add(typeDeclaration.getShortAliasName());
      }
    }
  }

  private Set<Import> toImports(Set<TypeDeclaration> typeDeclarations) {
    return typeDeclarations.stream().map(this::createImport).collect(toImmutableSet());
  }

  private Import createImport(TypeDeclaration typeDeclaration) {
    return new Import(computeAlias(typeDeclaration), typeDeclaration);
  }

  private String computeAlias(TypeDeclaration typeDeclaration) {
    if (typeDeclaration.isExtern()) {
      return typeDeclaration.getQualifiedJsName();
    }

    String shortAliasName = typeDeclaration.getShortAliasName();
    boolean unique = localNameUses.count(shortAliasName) == 1;
    return unique && JsProtectedNames.isLegalName(shortAliasName)
        ? shortAliasName
        : typeDeclaration.getLongAliasName();
  }
}
