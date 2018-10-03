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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
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
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.TypeVariable;
import com.google.j2cl.ast.UnionTypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationFragment;
import java.util.HashSet;
import java.util.List;
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
    return new ImportGatherer(declareLegacyNamespace).doGatherImports(type);
  }

  // TODO(b/80201427): We should also include TypeVariables on name recording.
  private final Set<String> localNameUses = new HashSet<>();

  private final Set<TypeDescriptor> collectedForJsDoc = new HashSet<>();

  private final SetMultimap<ImportCategory, TypeDeclaration> typeDeclarationByCategory =
      LinkedHashMultimap.create();

  private final boolean declareLegacyNamespace;

  private ImportGatherer(boolean declareLegacyNamespace) {
    this.declareLegacyNamespace = declareLegacyNamespace;
  }

  @Override
  public void exitField(Field field) {
    maybeAddNativeReference(field);
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
  public void exitFieldAccess(FieldAccess fieldAccess) {
    MemberDescriptor memberDescriptor = fieldAccess.getTarget();
    if (memberDescriptor.isExtern()) {
      addExternTypeDeclaration(memberDescriptor.getQualifiedJsName());
    }
  }

  @Override
  public void exitMethodCall(MethodCall methodCall) {
    MethodDescriptor methodDescriptor = methodCall.getTarget();
    if (methodCall.isStaticDispatch()) {
      addTypeDeclaration(methodDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration());
    }
    if (methodDescriptor.isExtern()) {
      addExternTypeDeclaration(methodDescriptor.getQualifiedJsName());
    }
  }

  @Override
  public void exitNewInstance(NewInstance newInstance) {
    addTypeDeclaration(newInstance.getTarget().getEnclosingTypeDescriptor().getTypeDeclaration());
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
    addTypeDeclaration(referencedTypeDeclaration);
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

    // JsDoc for types that are mapped directly into closure types.
    if (ClosureTypesGenerator.maybeGetStandardClosureType(
            declaredTypeDescriptor.getTypeDeclaration())
        != null) {
      return;
    }

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

  /** Adds a type declaration and figuring out whether is a LOADTIME or RUNTIME dependency. */
  private void addTypeDeclaration(TypeDeclaration typeDeclaration) {
    boolean isLoadTimeStatement = getCurrentMember().isField();
    addTypeDeclaration(
        typeDeclaration, isLoadTimeStatement ? ImportCategory.LOADTIME : ImportCategory.RUNTIME);
  }

  private void addTypeDeclaration(TypeDeclaration typeDeclaration, ImportCategory importCategory) {
    checkArgument(!typeDeclaration.isJsFunctionInterface());
    checkArgument(!typeDeclaration.getQualifiedJsName().isEmpty());

    typeDeclarationByCategory.put(
        typeDeclaration.isExtern() ? ImportCategory.EXTERN : importCategory,
        typeDeclaration.getEnclosingModule());
    // Reserve the name earlier on so that these are never aliased.
    if (typeDeclaration.isExtern()) {
      localNameUses.add(typeDeclaration.getEnclosingModule().getQualifiedJsName());
    }
  }

  private void addExternTypeDeclaration(String qualifiedJsName) {
    // Externs are references on the JsPackage.GLOBAL namespace and only the top scope qualifier is
    // relevant for avoiding collisions.
    String topScopeQualifier = Iterables.get(Splitter.on('.').split(qualifiedJsName), 0);
    // TODO(b/80488007): Refactor the way extern namespaces are handled.
    addTypeDeclaration(
        TypeDescriptors.createGlobalNativeTypeDescriptor(topScopeQualifier).getTypeDeclaration());
  }

  private Multimap<ImportCategory, Import> doGatherImports(Type type) {
    if (type.getDeclaration().isJsEnum()) {
      // TODO(b/116751296): Once the type model is refactored running this pass on a closure enum
      // should produce the right result without special handling.
      // Do not gather anything for Closure enums.
      return ImmutableMultimap.of(ImportCategory.SELF, createImport(type.getDeclaration()));
    }

    // Util class implements some utility functions and does not depend on any other class, always
    // import it eagerly.
    addTypeDeclaration(BootstrapType.NATIVE_UTIL.getDeclaration(), ImportCategory.LOADTIME);

    // Collect type references.
    type.accept(this);

    checkState(typeDeclarationByCategory.get(ImportCategory.SELF).size() == 1);

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
    return importsByCategory;
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

    List<String> nameComponents =
        ImmutableList.<String>builder()
            .add(getAbbreviatedPackageName(typeDeclaration))
            .addAll(getClassComponents(typeDeclaration))
            .build();

    // Construct an alias by starting from the inner most name (the name of the class and
    // prepending enclosing class names and the package name one at a time until there is
    // no conflict with an existing alias or a reserved keyword. For example when choosing an alias
    // for
    //
    //   class com.pack.MyClass.MyInnerClass
    //
    // the following sequence of aliases will be tried and the first one that does not collide will
    // be chosen.
    //
    //   MyInnerClass
    //   MyClass_MyInnerClass
    //   com_pack_MyClass_MyInnerClass
    //
    // There is an implicit assumption that if all the name parts are included the alias will
    // be unique in the same way that a Java fully qualified name corresponds exactly to one type.
    //
    String proposedAlias = null;
    do {
      checkState(!nameComponents.isEmpty());
      String lastComponent = Iterables.getLast(nameComponents);
      proposedAlias = Joiner.on('_').skipNulls().join(lastComponent, proposedAlias);

      nameComponents = nameComponents.subList(0, nameComponents.size() - 1);
    } while (localNameUses.contains(proposedAlias) || !JsProtectedNames.isLegalName(proposedAlias));

    localNameUses.add(proposedAlias);
    return proposedAlias;
  }

  /** Returns the class components normalizing the names of the internal runtime classes */
  private ImmutableList<String> getClassComponents(TypeDeclaration typeDeclaration) {
    if (BootstrapType.typeDescriptors.contains(typeDeclaration.toUnparamterizedTypeDescriptor())) {
      // Aliases for internal runtime classes are prepended '$' to their name to make them more
      // recognizable in the JavaScript source.
      return ImmutableList.of("$" + Iterables.getOnlyElement(typeDeclaration.getClassComponents()));
    }

    return typeDeclaration
        .getClassComponents()
        .stream()
        .map(component -> component.replace("_", "__"))
        .map(
            component ->
                !Character.isJavaIdentifierStart(component.charAt(0)) ? "$" + component : component)
        .collect(ImmutableList.toImmutableList());
  }

  private static final ImmutableMap<String, String> ABBREVIATED_WELL_KNOWN_PACKAGES =
      ImmutableMap.of(
          "java.lang",
          "j.l",
          "java.util",
          "j.u",
          "com.google.common",
          "c.g.c",
          "com.google",
          "c.g");

  private static String getAbbreviatedPackageName(TypeDeclaration typeDeclaration) {
    String packageName = typeDeclaration.getPackageName();
    for (String prefix : ABBREVIATED_WELL_KNOWN_PACKAGES.keySet()) {
      if (packageName.startsWith(prefix)) {
        packageName =
            ABBREVIATED_WELL_KNOWN_PACKAGES.get(prefix) + packageName.substring(prefix.length());
        break;
      }
    }
    return packageName.replace(".", "_");
  }
}
