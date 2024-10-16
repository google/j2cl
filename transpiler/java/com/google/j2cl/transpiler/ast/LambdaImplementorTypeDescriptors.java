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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Origin;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;

/** Utility TypeDescriptors methods used to synthesize lambda implementors. */
// TODO(b/63118697): Simplify this code once TD refactoring makes it easier to implement.
public final class LambdaImplementorTypeDescriptors {
  private static final String LAMBDA_IMPLEMENTOR_CLASS_NAME = "LambdaImplementor";

  /** Returns the TypeDescriptor for lambda instances of the functional interface. */
  public static DeclaredTypeDescriptor createLambdaImplementorTypeDescriptor(
      TypeDescriptor typeDescriptor,
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      int uniqueId,
      boolean capturesEnclosingInstance,
      List<TypeVariable> typeParameters,
      boolean extendsAbstractAdaptor) {

    // Lambdas that implement several types, e.g. from an intersection cast, require that all
    // those types be declared type descriptors.
    Stream<DeclaredTypeDescriptor> interfaceDescriptorsStream =
        typeDescriptor.isIntersection()
            ? ((IntersectionTypeDescriptor) typeDescriptor)
                .getIntersectionTypeDescriptors().stream().map(DeclaredTypeDescriptor.class::cast)
            : Stream.of((DeclaredTypeDescriptor) typeDescriptor);

    ImmutableList<DeclaredTypeDescriptor> interfaceTypeDescriptors =
        extendsAbstractAdaptor
            ? interfaceDescriptorsStream.collect(toImmutableList())
            : interfaceDescriptorsStream
                .map(LambdaImplementorTypeDescriptors::sanitizeDescriptor)
                .collect(toImmutableList());

    TypeDeclaration implementorTypeDeclaration =
        createLambdaImplementorTypeDeclaration(
            enclosingTypeDescriptor,
            interfaceTypeDescriptors,
            uniqueId,
            capturesEnclosingInstance,
            typeParameters,
            extendsAbstractAdaptor);

    return implementorTypeDeclaration.toDescriptor(typeParameters);
  }

  /** Sanitize the type arguments of a declared type descriptor. */
  private static DeclaredTypeDescriptor sanitizeDescriptor(DeclaredTypeDescriptor typeDescriptor) {
    // TODO(b/321074964): Do a more general recursive sanitizing removing all non-denotable types.
    var newTypeArguments =
        typeDescriptor.getTypeArgumentDescriptors().stream()
            .map(LambdaImplementorTypeDescriptors::toBounds)
            .collect(toImmutableList());

    if (newTypeArguments.equals(typeDescriptor.getTypeArgumentDescriptors())) {
      return typeDescriptor;
    }

    typeDescriptor = typeDescriptor.getDeclarationDescriptor();

    return (DeclaredTypeDescriptor)
        typeDescriptor.specializeTypeVariables(
            Streams.zip(
                    typeDescriptor.getTypeDeclaration().getTypeParameterDescriptors().stream(),
                    newTypeArguments.stream(),
                    Maps::immutableEntry)
                .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue)));
  }

  /** Projects wildcards and captures to their bounds. */
  private static TypeDescriptor toBounds(TypeDescriptor typeDescriptor) {
    if (!typeDescriptor.isTypeVariable()) {
      return typeDescriptor;
    }
    TypeVariable typeVariable = (TypeVariable) typeDescriptor;

    return typeVariable.isWildcardOrCapture()
        ? toBounds(
            typeVariable.getLowerBoundTypeDescriptor() != null
                ? typeVariable.getLowerBoundTypeDescriptor()
                : typeVariable.getUpperBoundTypeDescriptor())
        : typeVariable;
  }

  /** Returns the TypeDeclaration for the LambdaAdaptor class. */
  private static TypeDeclaration createLambdaImplementorTypeDeclaration(
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      List<DeclaredTypeDescriptor> interfaceTypeDescriptors,
      int uniqueId,
      boolean capturesEnclosingInstance,
      List<TypeVariable> typeParameters,
      boolean extendsAbstractAdaptor) {

    TypeDeclaration enclosingTypeDeclaration = enclosingTypeDescriptor.getTypeDeclaration();
    ImmutableList<String> classComponents =
        enclosingTypeDeclaration.synthesizeInnerClassComponents(
            LAMBDA_IMPLEMENTOR_CLASS_NAME, uniqueId);

    return TypeDeclaration.newBuilder()
        .setEnclosingTypeDeclaration(enclosingTypeDeclaration)
        .setSuperTypeDescriptorFactory(
            () -> getImplementorSupertype(interfaceTypeDescriptors, extendsAbstractAdaptor))
        .setClassComponents(classComponents)
        .setDeclaredMethodDescriptorsFactory(
            implementorTypeDeclaration ->
                ImmutableList.of(getLambdaMethod(implementorTypeDeclaration.toDescriptor())))
        .setInterfaceTypeDescriptorsFactory(() -> ImmutableList.copyOf(interfaceTypeDescriptors))
        .setTypeParameterDescriptors(typeParameters)
        .setVisibility(Visibility.PUBLIC)
        .setCapturingEnclosingInstance(capturesEnclosingInstance)
        .setKind(Kind.CLASS)
        .setAnonymous(true)
        .setOrigin(Origin.LAMBDA_IMPLEMENTOR)
        .build();
  }

  private static DeclaredTypeDescriptor getImplementorSupertype(
      List<DeclaredTypeDescriptor> interfaceTypeDescriptors, boolean extendsAbstractAdaptor) {
    return extendsAbstractAdaptor
        ? LambdaAdaptorTypeDescriptors.createAbstractLambdaAdaptorTypeDescriptor(
            // It is ok to choose any of its functional interfaces to create an abstract super type.
            // this accomplishes the main goal of inheriting class metadata. However the choice of
            // interface might have optimization implications that should be further explored.
            // TODO(b/360407114): See the effect of choosing a different one. In particular if
            // the interfaces are related it will be beneficial to choose the subtype.
            interfaceTypeDescriptors.stream()
                .filter(DeclaredTypeDescriptor::isFunctionalInterface)
                .findFirst()
                .get())
        : TypeDescriptors.get().javaLangObject;
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
        .makeDeclaration()
        // This is the declaration.
        .setEnclosingTypeDescriptor(implementorTypeDescriptor)
        .setSynthetic(false)
        .setAbstract(false)
        .build();
  }

  private LambdaImplementorTypeDescriptors() {}
}
