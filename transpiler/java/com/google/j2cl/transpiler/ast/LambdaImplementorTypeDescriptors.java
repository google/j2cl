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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import java.util.List;

/** Utility TypeDescriptors methods used to synthesize lambda implementors. */
// TODO(b/63118697): Simplify this code once TD refactoring makes it easier to implement.
public final class LambdaImplementorTypeDescriptors {
  private static final String FUNCTIONAL_INTERFACE_IMPLEMENTOR_CLASS_NAME = "LambdaImplementor";

  /** Returns the TypeDescriptor for lambda instances of the functional interface. */
  public static DeclaredTypeDescriptor createLambdaImplementorTypeDescriptor(
      TypeDescriptor typeDescriptor, DeclaredTypeDescriptor enclosingTypeDescriptor, int uniqueId) {
    return createLambdaImplementorTypeDescriptor(
        typeDescriptor, enclosingTypeDescriptor, uniqueId, false);
  }

  /** Returns the TypeDescriptor for lambda instances of the functional interface. */
  public static DeclaredTypeDescriptor createLambdaImplementorTypeDescriptor(
      TypeDescriptor typeDescriptor,
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      int uniqueId,
      boolean capturesEnclosingInstance) {

    DeclaredTypeDescriptor functionalInterfaceTypeDescriptor =
        typeDescriptor.getFunctionalInterface();

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
        createLambdaImplementorTypeDeclaration(
            typeDescriptor.toUnparameterizedTypeDescriptor(),
            enclosingTypeDescriptor.toUnparameterizedTypeDescriptor(),
            TypeDescriptors.toUnparameterizedTypeDescriptors(interfaceTypeDescriptors),
            uniqueId,
            capturesEnclosingInstance);

    return DeclaredTypeDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setTypeDeclaration(adaptorDeclaration)
        .setTypeArgumentDescriptors(functionalInterfaceTypeDescriptor.getTypeArgumentDescriptors())
        .setSuperTypeDescriptorFactory(() -> TypeDescriptors.get().javaLangObject)
        .setInterfaceTypeDescriptorsFactory(() -> ImmutableList.copyOf(interfaceTypeDescriptors))
        .setTypeArgumentDescriptors(typeArgumentDescriptors)
        .setDeclaredMethodDescriptorsFactory(
            implementorTypeDescriptor ->
                ImmutableList.of(getLambdaMethod(implementorTypeDescriptor)))
        .build();
  }

  /** Returns the TypeDeclaration for the LambdaAdaptor class. */
  private static TypeDeclaration createLambdaImplementorTypeDeclaration(
      TypeDescriptor lambdaTypeDescriptor,
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      List<DeclaredTypeDescriptor> interfaceTypeDescriptors,
      int uniqueId,
      boolean capturesEnclosingInstance) {

    TypeDeclaration enclosingTypeDeclaration = enclosingTypeDescriptor.getTypeDeclaration();
    ImmutableList<String> classComponents =
        enclosingTypeDeclaration.synthesizeInnerClassComponents(
            FUNCTIONAL_INTERFACE_IMPLEMENTOR_CLASS_NAME, uniqueId);

    ImmutableList<TypeVariable> typeParameterDescriptors =
        interfaceTypeDescriptors.stream()
            .flatMap(i -> i.getTypeDeclaration().getTypeParameterDescriptors().stream())
            .collect(toImmutableList());

    return TypeDeclaration.newBuilder()
        .setEnclosingTypeDeclaration(enclosingTypeDeclaration)
        .setSuperTypeDescriptorFactory(() -> TypeDescriptors.get().javaLangObject)
        .setClassComponents(classComponents)
        .setDeclaredMethodDescriptorsFactory(
            implementorTypeDeclaration ->
                ImmutableList.of(
                    getLambdaMethod(implementorTypeDeclaration.toUnparameterizedTypeDescriptor())))
        .setInterfaceTypeDescriptorsFactory(() -> ImmutableList.copyOf(interfaceTypeDescriptors))
        .setTypeParameterDescriptors(typeParameterDescriptors)
        .setUnparameterizedTypeDescriptorFactory(
            () ->
                createLambdaImplementorTypeDescriptor(
                    lambdaTypeDescriptor, enclosingTypeDescriptor, uniqueId))
        .setVisibility(Visibility.PUBLIC)
        .setCapturingEnclosingInstance(capturesEnclosingInstance)
        .setKind(Kind.CLASS)
        .setAnonymous(true)
        .build();
  }

  /** Returns the MethodDescriptor for the SAM implementation in the LambdaImplementor class. */
  @SuppressWarnings("ReferenceEquality")
  private static MethodDescriptor getLambdaMethod(
      DeclaredTypeDescriptor implementorTypeDescriptor) {
    DeclaredTypeDescriptor functionalInterfaceTypeDescriptor =
        implementorTypeDescriptor.getFunctionalInterface();
    checkState(
        functionalInterfaceTypeDescriptor.getFunctionalInterface()
            == functionalInterfaceTypeDescriptor);

    MethodDescriptor functionalInterfaceMethodDescriptor =
        functionalInterfaceTypeDescriptor.getSingleAbstractMethodDescriptor();
    return MethodDescriptor.Builder.from(functionalInterfaceMethodDescriptor)
        .setNative(false)
        // This is the declaration.
        .setDeclarationDescriptor(null)
        .setEnclosingTypeDescriptor(implementorTypeDescriptor)
        .setSynthetic(false)
        .setAbstract(false)
        .build();
  }

  private LambdaImplementorTypeDescriptors() {}
}
