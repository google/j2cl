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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Origin;
import java.util.List;
import java.util.Optional;

/** Utility TypeDescriptors methods related to lambda synthesis. */
// TODO(b/63118697): Simplify this code once TD refactoring makes it easier to implement.
public final class LambdaAdaptorTypeDescriptors {
  private static final String FUNCTIONAL_INTERFACE_JSFUNCTION_CLASS_NAME = "JsFunction";
  private static final String FUNCTIONAL_INTERFACE_ADAPTOR_CLASS_NAME = "LambdaAdaptor";

  /** Returns the TypeDescriptor for an abstract lambda implementor super class. */
  public static DeclaredTypeDescriptor createAbstractLambdaAdaptorTypeDescriptor(
      TypeDescriptor typeDescriptor) {
    return createLambdaAdaptorTypeDescriptor(
        typeDescriptor, (DeclaredTypeDescriptor) typeDescriptor, true, Optional.empty());
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
        typeDescriptor, enclosingTypeDescriptor, false, uniqueId);
  }

  /** Returns the TypeDescriptor for lambda instances of the functional interface. */
  private static DeclaredTypeDescriptor createLambdaAdaptorTypeDescriptor(
      TypeDescriptor typeDescriptor,
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      boolean isAbstract,
      Optional<Integer> uniqueId) {

    DeclaredTypeDescriptor functionalInterfaceTypeDescriptor =
        typeDescriptor.getFunctionalInterface();

    checkArgument(!functionalInterfaceTypeDescriptor.isJsFunctionInterface());

    DeclaredTypeDescriptor jsFunctionInterface =
        createJsFunctionTypeDescriptor(functionalInterfaceTypeDescriptor);

    // Lambdas that implement several types, e.g. from an intersection cast, require that all
    // those types be declared type descriptors.
    List<DeclaredTypeDescriptor> interfaceTypeDescriptors =
        typeDescriptor.isIntersection()
            ? ((IntersectionTypeDescriptor) typeDescriptor)
                .getIntersectionTypeDescriptors().stream()
                    .map(DeclaredTypeDescriptor.class::cast)
                    .collect(toImmutableList())
            : ImmutableList.of((DeclaredTypeDescriptor) typeDescriptor);

    ImmutableList<TypeDescriptor> typeArgumentDescriptors =
        interfaceTypeDescriptors.stream()
            .flatMap(i -> i.getTypeArgumentDescriptors().stream())
            .collect(toImmutableList());

    TypeDeclaration adaptorDeclaration =
        createLambdaAdaptorTypeDeclaration(
            enclosingTypeDescriptor.getDeclarationDescriptor(),
            TypeDescriptors.getDeclarationDescriptors(interfaceTypeDescriptors),
            jsFunctionInterface.getDeclarationDescriptor(),
            isAbstract,
            uniqueId);

    return adaptorDeclaration.toDescriptor(typeArgumentDescriptors);
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
        getAdaptorForwardingMethod(adaptorTypeDescriptor));
  }

  /** Returns the TypeDeclaration for the LambdaAdaptor class. */
  private static TypeDeclaration createLambdaAdaptorTypeDeclaration(
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      List<DeclaredTypeDescriptor> interfaceTypeDescriptors,
      DeclaredTypeDescriptor jsFunctionInterface,
      boolean isAbstract,
      Optional<Integer> uniqueId) {

    TypeDeclaration enclosingTypeDeclaration = enclosingTypeDescriptor.getTypeDeclaration();
    ImmutableList<String> classComponents =
        enclosingTypeDeclaration.synthesizeInnerClassComponents(
            FUNCTIONAL_INTERFACE_ADAPTOR_CLASS_NAME, uniqueId.orElse(null));

    ImmutableList<TypeVariable> typeParameterDescriptors =
        interfaceTypeDescriptors.stream()
            .flatMap(i -> i.getTypeDeclaration().getTypeParameterDescriptors().stream())
            .collect(toImmutableList());
    ;

    return TypeDeclaration.newBuilder()
        .setEnclosingTypeDeclaration(enclosingTypeDeclaration)
        .setSuperTypeDescriptorFactory(() -> TypeDescriptors.get().javaLangObject)
        .setClassComponents(classComponents)
        .setDeclaredMethodDescriptorsFactory(
            adaptorTypeDeclaration ->
                isAbstract
                    ? ImmutableList.of()
                    : getLambdaAdaptorMethodDescriptors(
                        jsFunctionInterface, adaptorTypeDeclaration.toDescriptor()))
        .setInterfaceTypeDescriptorsFactory(() -> ImmutableList.copyOf(interfaceTypeDescriptors))
        .setTypeParameterDescriptors(typeParameterDescriptors)
        .setHasAbstractModifier(isAbstract)
        .setVisibility(Visibility.PUBLIC)
        .setKind(Kind.CLASS)
        .setOrigin(Origin.LAMBDA_ABSTRACT_ADAPTOR)
        .build();
  }

  /** Returns the MethodDescriptor for the constructor of the LambdaAdaptor class. */
  private static MethodDescriptor getLambdaAdaptorConstructor(
      DeclaredTypeDescriptor jsFunctionInterface, DeclaredTypeDescriptor adaptorTypeDescriptor) {
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(adaptorTypeDescriptor)
        .setConstructor(true)
        .setOriginalJsInfo(JsInfo.RAW_CTOR)
        .setOrigin(MethodDescriptor.MethodOrigin.SYNTHETIC_LAMBDA_ADAPTOR_CONSTRUCTOR)
        .setParameterTypeDescriptors(jsFunctionInterface)
        .build();
  }

  /** Returns the MethodDescriptor for the SAM implementation in the LambdaAdaptor class. */
  @SuppressWarnings("ReferenceEquality")
  public static MethodDescriptor getAdaptorForwardingMethod(
      DeclaredTypeDescriptor adaptorTypeDescriptor) {
    DeclaredTypeDescriptor functionalInterfaceTypeDescriptor =
        adaptorTypeDescriptor.getFunctionalInterface();
    checkState(
        functionalInterfaceTypeDescriptor.getFunctionalInterface()
            == functionalInterfaceTypeDescriptor);

    MethodDescriptor functionalInterfaceMethodDescriptor =
        functionalInterfaceTypeDescriptor.getSingleAbstractMethodDescriptor();
    return MethodDescriptor.Builder.from(functionalInterfaceMethodDescriptor)
        .setDeclarationDescriptor(null)
        .setEnclosingTypeDescriptor(adaptorTypeDescriptor)
        // Remove the method type parameters as they when moved to the adaptor type.
        .setTypeParameterTypeDescriptors(ImmutableList.of())
        .setTypeArgumentTypeDescriptors(ImmutableList.of())
        .setSynthetic(false)
        .setAbstract(false)
        .setNative(false)
        .build();
  }

  /** Returns the TypeDescriptor for lambda instances of the functional interface. */
  public static DeclaredTypeDescriptor createJsFunctionTypeDescriptor(
      DeclaredTypeDescriptor functionalTypeDescriptor) {
    checkArgument(!functionalTypeDescriptor.isJsFunctionInterface());

    return createJsFunctionTypeDeclaration(functionalTypeDescriptor)
        .toDescriptor(functionalTypeDescriptor.getTypeArgumentDescriptors());
  }

  /** Returns the TypeDeclaration for the JsFunction class. */
  private static TypeDeclaration createJsFunctionTypeDeclaration(
      DeclaredTypeDescriptor functionalTypeDescriptor) {

    TypeDeclaration typeDeclaration = functionalTypeDescriptor.getTypeDeclaration();
    ImmutableList<String> classComponents =
        typeDeclaration.synthesizeInnerClassComponents(FUNCTIONAL_INTERFACE_JSFUNCTION_CLASS_NAME);

    return TypeDeclaration.newBuilder()
        .setEnclosingTypeDeclaration(typeDeclaration)
        .setTypeParameterDescriptors(typeDeclaration.getTypeParameterDescriptors())
        .setClassComponents(classComponents)
        .setJsFunctionInterface(true)
        .setFunctionalInterface(true)
        .setDeclaredMethodDescriptorsFactory(
            jsfunctionTypeDeclaration ->
                ImmutableList.of(
                    createJsFunctionMethodDescriptor(
                        jsfunctionTypeDeclaration.toDescriptor(),
                        typeDeclaration.getSingleAbstractMethodDescriptor())))
        .setSingleAbstractMethodDescriptorFactory(t -> t.getDeclaredMethodDescriptors().get(0))
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
                .map(
                    parameterDescriptor ->
                        ParameterDescriptor.newBuilder()
                            .setTypeDescriptor(parameterDescriptor.getTypeDescriptor())
                            .build())
                .collect(toImmutableList());

    return MethodDescriptor.Builder.from(singleAbstractMethod)
        .setDeclarationDescriptor(null)
        .setEnclosingTypeDescriptor(jsfunctionTypeDescriptor)
        .setParameterDescriptors(parameterDescriptors)
        .setOriginalJsInfo(
            JsInfo.newBuilder()
                .setJsMemberType(JsMemberType.NONE)
                .setJsAsync(singleAbstractMethod.getJsInfo().isJsAsync())
                .build())
        .setNative(false)
        .build();
  }

  private LambdaAdaptorTypeDescriptors() {}
}
