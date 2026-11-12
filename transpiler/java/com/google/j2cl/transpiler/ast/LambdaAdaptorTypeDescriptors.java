/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.function.Predicate.not;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Origin;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/** Utility TypeDescriptors methods related to lambda synthesis. */
// TODO(b/63118697): Simplify this code once TD refactoring makes it easier to implement.
public final class LambdaAdaptorTypeDescriptors {
  private static final String FUNCTIONAL_INTERFACE_JSFUNCTION_CLASS_NAME = "JsFunction";
  private static final String FUNCTIONAL_INTERFACE_ADAPTOR_CLASS_NAME = "LambdaAdaptor";

  private enum AdaptorKind {
    CONCRETE,
    ABSTRACT,
  }

  /** Returns the TypeDescriptor for a Wasm base lambda adaptor class. */
  public static DeclaredTypeDescriptor createFunctionalInterfaceAdaptorTypeDescriptor(
      TypeDescriptor typeDescriptor) {
    checkArgument(typeDescriptor.isFunctionalInterface());
    return createLambdaAdaptorTypeDescriptor(
        typeDescriptor,
        (DeclaredTypeDescriptor) typeDescriptor,
        AdaptorKind.ABSTRACT,
        Optional.empty());
  }

  /** Returns the TypeDescriptor for a LambdaAdaptor class. */
  public static DeclaredTypeDescriptor createLambdaAdaptorTypeDescriptor(
      TypeDescriptor typeDescriptor) {
    return createLambdaAdaptorTypeDescriptor(
        typeDescriptor, (DeclaredTypeDescriptor) typeDescriptor, Optional.empty());
  }

  /** Returns the TypeDescriptor for lambda instances of the functional interface. */
  public static DeclaredTypeDescriptor createLambdaAdaptorTypeDescriptor(
      TypeDescriptor typeDescriptor,
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      Optional<Integer> uniqueId) {
    return createLambdaAdaptorTypeDescriptor(
        typeDescriptor, enclosingTypeDescriptor, AdaptorKind.CONCRETE, uniqueId);
  }

  /** Returns the TypeDescriptor for lambda instances of the functional interface. */
  private static DeclaredTypeDescriptor createLambdaAdaptorTypeDescriptor(
      TypeDescriptor typeDescriptor,
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      AdaptorKind adaptorKind,
      Optional<Integer> uniqueId) {

    DeclaredTypeDescriptor jsFunctionInterface = createJsFunctionTypeDescriptor(typeDescriptor);
    List<DeclaredTypeDescriptor> interfaceTypeDescriptors =
        getImplementedInterfaceTypeDescriptors(typeDescriptor);

    ImmutableList<TypeDescriptor> typeArgumentDescriptors =
        interfaceTypeDescriptors.stream()
            .flatMap(i -> i.getTypeArgumentDescriptors().stream())
            .collect(toImmutableList());

    TypeDeclaration adaptorDeclaration =
        createLambdaAdaptorTypeDeclaration(
            enclosingTypeDescriptor.getDeclarationDescriptor(),
            TypeDescriptors.getDeclarationDescriptors(interfaceTypeDescriptors),
            jsFunctionInterface.getDeclarationDescriptor(),
            adaptorKind,
            uniqueId);

    return adaptorDeclaration.toDescriptor(typeArgumentDescriptors);
  }

  /**
   * Returns the interfaces implemented by lambdas that target {@code typeDescriptor}, i.e. the type
   * itself or the components of an intersection, excluding {@code Object} which can be the class
   * component of an intersection that is functional.
   */
  static ImmutableList<DeclaredTypeDescriptor> getImplementedInterfaceTypeDescriptors(
      TypeDescriptor typeDescriptor) {
    return typeDescriptor instanceof IntersectionTypeDescriptor intersectionTypeDescriptor
        ? intersectionTypeDescriptor.getIntersectionTypeDescriptors().stream()
            .filter(not(TypeDescriptors::isJavaLangObject))
            .map(DeclaredTypeDescriptor.class::cast)
            .collect(toImmutableList())
        : ImmutableList.of((DeclaredTypeDescriptor) typeDescriptor);
  }

  /**
   * Returns the functional interface implemented by lambdas that target {@code typeDescriptor}
   * whose single abstract method is the function type, or {@code null} if there is none.
   */
  @Nullable
  public static DeclaredTypeDescriptor findFunctionalInterface(TypeDescriptor typeDescriptor) {
    return findFunctionalInterface(getImplementedInterfaceTypeDescriptors(typeDescriptor));
  }

  /**
   * Returns the functional interface in {@code interfaceTypeDescriptors} whose single abstract
   * method is the function type of their intersection, or {@code null} if there is none.
   *
   * <p>Lambdas that target such an interface can share its adaptor and synthetic JsFunction type.
   */
  @Nullable
  @SuppressWarnings("ReferenceEquality")
  static DeclaredTypeDescriptor findFunctionalInterface(
      List<DeclaredTypeDescriptor> interfaceTypeDescriptors) {
    MethodDescriptor functionalMethod =
        AstUtils.getFunctionalMethodDescriptor(interfaceTypeDescriptors);
    if (functionalMethod == null) {
      return null;
    }
    return interfaceTypeDescriptors.stream()
        .filter(i ->
            i.isFunctionalInterface()
                && i.getFunctionalMethodDescriptor().getDeclarationDescriptor()
                    == functionalMethod.getDeclarationDescriptor())
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns the method descriptor for the methods in the LambdaAdaptor class.
   *
   * <p>The LambdaAdaptor class has two methods:
   * <li>a constructor taking the synthetic @JsFunction interface
   * <li>a SAM method implementation forwarding to the @JsFunction interface.
   */
  private static ImmutableList<MethodDescriptor> getLambdaAdaptorMethodDescriptors(
      DeclaredTypeDescriptor jsFunctionInterface, DeclaredTypeDescriptor adaptorTypeDescriptor) {
    return ImmutableList.of(
        getLambdaAdaptorConstructor(jsFunctionInterface, adaptorTypeDescriptor),
        getFunctionalMethodImplementation(adaptorTypeDescriptor));
  }

  /** Returns the TypeDeclaration for the LambdaAdaptor class. */
  private static TypeDeclaration createLambdaAdaptorTypeDeclaration(
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      List<DeclaredTypeDescriptor> interfaceTypeDescriptors,
      DeclaredTypeDescriptor jsFunctionInterface,
      AdaptorKind adaptorKind,
      Optional<Integer> uniqueId) {

    TypeDeclaration enclosingTypeDeclaration = enclosingTypeDescriptor.getTypeDeclaration();
    ImmutableList<String> classComponents =
        enclosingTypeDeclaration.synthesizeInnerClassComponents(
            FUNCTIONAL_INTERFACE_ADAPTOR_CLASS_NAME, uniqueId.orElse(null));

    ImmutableList<TypeVariable> typeParameterDescriptors =
        interfaceTypeDescriptors.stream()
            .flatMap(i -> i.getTypeDeclaration().getTypeParameterDescriptors().stream())
            .collect(toImmutableList());

    return TypeDeclaration.builder()
        .setEnclosingTypeDeclaration(enclosingTypeDeclaration)
        .setSuperTypeDescriptorFactory(() -> TypeDescriptors.get().javaLangObject)
        .setClassComponents(classComponents)
        .setDeclaredMethodDescriptorsFactory(adaptorTypeDeclaration ->
            switch (adaptorKind) {
              case CONCRETE ->
                  getLambdaAdaptorMethodDescriptors(
                      jsFunctionInterface, adaptorTypeDeclaration.toDescriptor());
              case ABSTRACT -> ImmutableList.of();
            })
        .setInterfaceTypeDescriptorsFactory(() -> ImmutableList.copyOf(interfaceTypeDescriptors))
        .setTypeParameterDescriptors(typeParameterDescriptors)
        .setHasAbstractModifier(adaptorKind == AdaptorKind.ABSTRACT)
        .setVisibility(Visibility.PUBLIC)
        .setKind(Kind.CLASS)
        .setOrigin(Origin.LAMBDA_ABSTRACT_ADAPTOR)
        .build();
  }

  /** Returns the MethodDescriptor for the constructor of the LambdaAdaptor class. */
  private static MethodDescriptor getLambdaAdaptorConstructor(
      DeclaredTypeDescriptor jsFunctionInterface, DeclaredTypeDescriptor adaptorTypeDescriptor) {
    return MethodDescriptor.builder()
        .setEnclosingTypeDescriptor(adaptorTypeDescriptor)
        .setConstructor(true)
        .setOriginalJsInfo(JsInfo.RAW_CTOR)
        .setOrigin(MethodDescriptor.MethodOrigin.SYNTHETIC_LAMBDA_ADAPTOR_CONSTRUCTOR)
        .setParameterDescriptors(ParameterDescriptor.create("fn", jsFunctionInterface))
        .build();
  }

  /**
   * Returns the MethodDescriptor for the implementation of the functional method in a synthetic
   * lambda class, i.e. an adaptor or implementor, whose interfaces are those of the lambda target.
   */
  public static MethodDescriptor getFunctionalMethodImplementation(
      DeclaredTypeDescriptor typeDescriptor) {
    MethodDescriptor functionalMethodDescriptor =
        AstUtils.getFunctionalMethodDescriptor(typeDescriptor.getInterfaceTypeDescriptors());
    return functionalMethodDescriptor.toBuilder()
        .setDeclarationDescriptor(null)
        .setEnclosingTypeDescriptor(typeDescriptor)
        .setOrigin(MethodOrigin.SYNTHETIC_METHOD)
        .setAbstract(false)
        .setNative(false)
        .build();
  }

  /** Returns the synthetic JsFunction interface for the function type of {@code typeDescriptor}. */
  public static DeclaredTypeDescriptor createJsFunctionTypeDescriptor(
      TypeDescriptor typeDescriptor) {
    DeclaredTypeDescriptor functionalInterface =
        findFunctionalInterface(getImplementedInterfaceTypeDescriptors(typeDescriptor));
    checkArgument(!functionalInterface.isJsFunctionInterface());

    return createJsFunctionTypeDeclaration(functionalInterface)
        .toDescriptor(functionalInterface.getTypeArgumentDescriptors());
  }

  /** Returns the TypeDeclaration for the JsFunction class. */
  private static TypeDeclaration createJsFunctionTypeDeclaration(
      DeclaredTypeDescriptor functionalTypeDescriptor) {

    TypeDeclaration typeDeclaration = functionalTypeDescriptor.getTypeDeclaration();
    ImmutableList<String> classComponents =
        typeDeclaration.synthesizeInnerClassComponents(FUNCTIONAL_INTERFACE_JSFUNCTION_CLASS_NAME);

    return TypeDeclaration.builder()
        .setEnclosingTypeDeclaration(typeDeclaration)
        .setTypeParameterDescriptors(typeDeclaration.getTypeParameterDescriptors())
        .setClassComponents(classComponents)
        .setJsFunctionInterface()
        .setDeclaredMethodDescriptorsFactory(jsfunctionTypeDeclaration ->
            ImmutableList.of(
                createJsFunctionMethodDescriptor(
                    jsfunctionTypeDeclaration.toDescriptor(),
                    typeDeclaration.getSingleAbstractMethodDescriptor())))
        .setSingleAbstractMethodDescriptorFactory(t -> t.getDeclaredMethodDescriptors().getFirst())
        .setVisibility(Visibility.PUBLIC)
        .setKind(Kind.INTERFACE)
        .build();
  }

  /** Returns the MethodDescriptor for the single method in the synthetic @JsFunction interface. */
  private static MethodDescriptor createJsFunctionMethodDescriptor(
      DeclaredTypeDescriptor jsfunctionTypeDescriptor, MethodDescriptor singleAbstractMethod) {
    // Remove the type parameters in the functional method since they are not allowed in
    // @JsFunction interfaces.
    singleAbstractMethod = singleAbstractMethod.withoutTypeParameters();

    // Remove varargs if the functional method is not a JsMethod, otherwise it will become
    // JsVarargs due to being varargs in a JsFunction, that will cause it to loose
    // runtime type checking on the varargs parameter.
    var parameterDescriptors =
        singleAbstractMethod.isJsMethod()
            ? singleAbstractMethod.getParameterDescriptors()
            : singleAbstractMethod.getParameterDescriptors().stream()
                .map(parameterDescriptor ->
                    parameterDescriptor.toBuilder().setVarargs(false).build())
                .collect(toImmutableList());

    return singleAbstractMethod.toBuilder()
        .setDeclarationDescriptor(null)
        .setEnclosingTypeDescriptor(jsfunctionTypeDescriptor)
        .setParameterDescriptors(parameterDescriptors)
        .setOriginalJsInfo(JsInfo.NONE)
        .setNative(false)
        .build();
  }

  private LambdaAdaptorTypeDescriptors() {}
}
