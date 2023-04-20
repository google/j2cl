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
package com.google.j2cl.transpiler.backend.closure;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDeclarationStatement;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.backend.closure.Import.ImportCategory;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Traverses a Type, gathers imports for all things it references and creates non colliding local
 * aliases for each import.
 */
class ImportGatherer extends AbstractVisitor {

  public static List<Import> gatherImports(Type type) {
    return new ImportGatherer().doGatherImports(type);
  }

  // TODO(b/80201427): We should also include TypeVariables on name recording.
  private final Set<String> localNameUses = new HashSet<>();

  private final Set<TypeDescriptor> collectedForJsDoc = new HashSet<>();

  private final Map<TypeDeclaration, ImportCategory> categoryForTypeDeclaration =
      new LinkedHashMap<>();

  private ImportGatherer() {}

  @Override
  public void exitFunctionExpression(FunctionExpression functionExpression) {
    for (Variable parameter : functionExpression.getParameters()) {
      collectForJsDoc(parameter.getTypeDescriptor());
    }
  }

  @Override
  public void exitType(Type type) {
    addTypeDeclaration(type.getDeclaration(), ImportCategory.SELF);

    if (type.isOverlayImplementation() && type.getOverlaidTypeDeclaration().isNative()) {
      collectImportsFromNativeType(type.getOverlaidTypeDeclaration());
    }

    if (type.getSuperTypeDescriptor() != null) {
      // The superclass appears in the extends clause, hence collect for loadtime.
      addTypeDeclaration(
          type.getSuperTypeDescriptor().getTypeDeclaration(), ImportCategory.LOADTIME);
    }

    // All supertypes, including the super class, need to be collected for JsDoc, so that the types
    // themselves and all the types they might reference as type arguments are properly imported,
    // that includes interfaces that are only in @implements and type arguments for the
    // superclass which appear in the @extends.
    type.getSuperTypesStream().forEach(this::collectForJsDoc);
  }

  /**
   * Collects types that need to be imported from native types.
   *
   * <p>Overlay types synthesized from native types might not refer to types that might be exposed
   * by the native type. So here all the relevant imports are collected directly from the native
   * type declaration.
   */
  private void collectImportsFromNativeType(TypeDeclaration nativeType) {
    // The synthesized JsOverlayImpl type should import the native type eagerly for $isInstance.
    // Also requiring native type makes sure the native type is not pruned by AJD.
    addTypeDeclaration(nativeType, ImportCategory.LOADTIME);
    // Collect types that could be reached through chaining methods or fields. Through chaining
    // one can access devirtualized overlay methods and static methods, and those need a direct
    // reference to the declaring class at the usage site. In order of AJD to preserve those,
    // wherever they are exposed they need to be required.
    nativeType
        .toUnparameterizedTypeDescriptor()
        .getDeclaredMemberDescriptors()
        .forEach(
            m -> {
              maybeAddNativeReference(m);
              if (m.isMethod()) {
                collectForJsDoc(((MethodDescriptor) m).getReturnTypeDescriptor());
              } else if (m.isField()) {
                collectForJsDoc(((FieldDescriptor) m).getTypeDescriptor());
              }
            });
  }

  @Override
  public void exitJsDocCastExpression(JsDocCastExpression jsDocCastExpression) {
    collectForJsDoc(jsDocCastExpression.getTypeDescriptor());
  }

  @Override
  public void exitFieldDeclarationStatement(FieldDeclarationStatement fieldDeclarationStatement) {
    collectForJsDoc(fieldDeclarationStatement.getFieldDescriptor().getTypeDescriptor());
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
    MethodDescriptor methodDescriptor = method.getDescriptor();
    maybeAddNativeReference(methodDescriptor);

    collectForJsDoc(methodDescriptor.getReturnTypeDescriptor());
    // Collect the types of the parameters from the method descriptor as those are the ones
    // used for the inline @param declaration. Do not collect explicitly the types of the
    // Variable object used to represent the parameter in the AST since its type is never used
    // implicitly and might be different from the type in the method descriptor (as the bridge
    // construction algorithm leverages the fact that the type of the parameter variable can be
    // different from the parameter type declaration to implement the erasure cast checks).
    methodDescriptor.getParameterTypeDescriptors().forEach(this::collectForJsDoc);
  }

  private void maybeAddNativeReference(MemberDescriptor memberDescriptor) {
    if (memberDescriptor.hasJsNamespace() && !memberDescriptor.isExtern()) {
      // The member exposes an unrelated namespace that needs to be imported in order to prevent
      // it from beign pruned by AJD.
      addTypeDeclaration(
          AstUtils.getNamespaceAsTypeDescriptor(memberDescriptor).getTypeDeclaration(),
          ImportCategory.JSDOC);
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

  @Override
  public void exitInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
    // At this point only a declared type can appear as the type in the lhs of an instanceof.
    // Arrays, the only other type that can appear as a rhs of an instanceof, have been
    // normalized away.
    DeclaredTypeDescriptor testTypeDescriptor =
        (DeclaredTypeDescriptor) instanceOfExpression.getTestTypeDescriptor();
    addTypeDeclaration(testTypeDescriptor.getTypeDeclaration());
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
    if (declaredTypeDescriptor.hasOverlayImplementationType()) {
      addTypeDeclaration(
          declaredTypeDescriptor.getOverlayImplementationTypeDescriptor().getTypeDeclaration(),
          ImportCategory.AJD_DEPENDENCY);
    }

    if (AstUtils.isNonNativeJsEnum(typeDescriptor)) {
      collectForJsDoc(TypeDescriptors.getEnumBoxType(typeDescriptor.toRawTypeDescriptor()));
    }

    if (declaredTypeDescriptor.isJsFunctionInterface()) {
      // Both JsFunction interfaces emit a function signature that is derived from the functional
      // method. Such method might refer to types that need to be collected.
      collectTypeDescriptorsIntroducedByJsFunction(declaredTypeDescriptor);

      // In contrast to other native classes, JsFunction classes do not exist at all at runtime.
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
    checkState(typeDescriptor.isJsFunctionInterface());
    MethodDescriptor jsFunctionMethodDescriptor = typeDescriptor.getJsFunctionMethodDescriptor();
    for (TypeDescriptor parameterTypeDescriptor :
        jsFunctionMethodDescriptor.getParameterTypeDescriptors()) {
      collectForJsDoc(parameterTypeDescriptor);
    }
    collectForJsDoc(jsFunctionMethodDescriptor.getReturnTypeDescriptor());
  }

  /**
   * Type upper bounds may be implicitly referred from other compilation units that are compiled
   * separately due to synthesized erasure casts. In order for these classes to be preserved and not
   * pruned by AJD, we should create a dependency to the bound.
   */
  private void collectTypeDescriptorsIntroducedByTypeBounds(TypeVariable typeVariable) {
    TypeDescriptor boundTypeDescriptor = typeVariable.getUpperBoundTypeDescriptor();

    if (TypeDescriptors.isJavaLangObject(boundTypeDescriptor)) {
      // Effectively unbounded and will not result in erasure casts.
      return;
    }

    collectForJsDoc(boundTypeDescriptor);
  }

  /** Adds a type declaration and figuring out whether is a LOADTIME or RUNTIME dependency. */
  private void addTypeDeclaration(TypeDeclaration typeDeclaration) {
    addTypeDeclaration(typeDeclaration, getImplicitImportCategory());
  }

  private void addTypeDeclaration(TypeDeclaration typeDeclaration, ImportCategory importCategory) {
    checkArgument(!typeDeclaration.isJsFunctionInterface());
    checkArgument(!typeDeclaration.getQualifiedJsName().isEmpty());

    categoryForTypeDeclaration.merge(
        typeDeclaration.getEnclosingModule(),
        typeDeclaration.isExtern() ? ImportCategory.EXTERN : importCategory,
        (existing, other) -> existing == null || other.strongerThan(existing) ? other : existing);

    // Reserve the name earlier on so that these are never aliased.
    if (typeDeclaration.isExtern()) {
      localNameUses.add(typeDeclaration.getEnclosingModule().getQualifiedJsName());
    }
  }

  /**
   * Determines the import category of a type descriptor appearing in the code depending on the
   * context.
   */
  private ImportCategory getImplicitImportCategory() {

    if (getCurrentMember() == null) {
      // The reference appears in a load time statement, hence needs a LOADTIME import category.
      return ImportCategory.LOADTIME;
    }

    if (getCurrentMember().isField()) {
      // The reference appears in a field initializer, hence it needs a LOADTIME import category.
      // Note that at this stage all field initialization has already been moved to the constructor
      // or $clinit. Hence the initializers that read this point are the default values for
      // their types and the compiler type constancts. The only type reference that needs and import
      // and that can appear here is the type for primitive longs.
      return ImportCategory.LOADTIME;
    }

    if (getCurrentMember().getDescriptor().getOrigin().isInstanceOfSupportMember()) {
      // InstanceOf support members, e.g. $isInstance, might not call $clinit hence all the
      // types used in their implementation need to be imported as LOADTIME.
      return ImportCategory.LOADTIME;
    }

    return ImportCategory.RUNTIME;
  }

  private void addExternTypeDeclaration(String qualifiedJsName) {
    // Externs are references on the JsPackage.GLOBAL namespace and only the top scope qualifier is
    // relevant for avoiding collisions.
    String topScopeQualifier = Iterables.get(Splitter.on('.').split(qualifiedJsName), 0);
    // TODO(b/80488007): Refactor the way extern namespaces are handled.
    addTypeDeclaration(
        TypeDescriptors.createGlobalNativeTypeDescriptor(topScopeQualifier).getTypeDeclaration());
  }

  private ImmutableList<Import> doGatherImports(Type type) {
    // Collect type references.
    type.accept(this);

    return categoryForTypeDeclaration.entrySet().stream()
        .map(entry -> createImport(entry.getKey(), entry.getValue()))
        .sorted()
        .collect(ImmutableList.toImmutableList());
  }

  private Import createImport(TypeDeclaration typeDeclaration, ImportCategory importCategory) {
    return new Import(computeAlias(typeDeclaration), typeDeclaration, importCategory);
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
    } while (localNameUses.contains(proposedAlias) || !JsKeywords.isKeyword(proposedAlias));

    localNameUses.add(proposedAlias);
    return proposedAlias;
  }

  /** Returns the class components normalizing the names of the internal runtime classes */
  private ImmutableList<String> getClassComponents(TypeDeclaration typeDeclaration) {
    if (typeDeclaration.getQualifiedJsName().startsWith("vmbootstrap.")
        || typeDeclaration.getQualifiedJsName().startsWith("nativebootstrap.")) {
      // Aliases for internal runtime classes are prepended '$' to their name to make them more
      // recognizable in the JavaScript source.
      return ImmutableList.of("$" + String.join("_", typeDeclaration.getClassComponents()));
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
    return packageName.replace('.', '_');
  }
}
