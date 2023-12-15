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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Context;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** A node that represents a Java Type declaration in the compilation unit. */
@Visitable
@Context
public class Type extends Node implements HasSourcePosition, HasJsNameInfo, HasReadableDescription {
  private final TypeDeclaration typeDeclaration;
  @Visitable List<Member> members = new ArrayList<>();
  @Visitable List<Type> types = new ArrayList<>();
  @Visitable List<Statement> loadTimeStatements = new ArrayList<>();
  private final SourcePosition sourcePosition;
  private boolean isAbstract;
  private DeclaredTypeDescriptor superTypeDescriptor;
  private boolean isOptimizedEnum;

  public Type(SourcePosition sourcePosition, TypeDeclaration typeDeclaration) {
    this.sourcePosition = checkNotNull(sourcePosition);
    checkArgument(
        typeDeclaration.isInterface() || typeDeclaration.isClass() || typeDeclaration.isEnum());
    this.typeDeclaration = typeDeclaration;
    this.isAbstract = typeDeclaration.isAbstract();
    this.superTypeDescriptor = typeDeclaration.getSuperTypeDescriptor();
  }

  /**
   * Returns the TypeDescriptor for this Type parametrized by the type variables from the
   * declaration.
   */
  public DeclaredTypeDescriptor getTypeDescriptor() {
    return getDeclaration().toUnparameterizedTypeDescriptor();
  }

  public boolean containsMethod(String mangledName) {
    return containsMethod(method -> method.getMangledName().equals(mangledName));
  }

  public boolean containsMethod(Predicate<MethodDescriptor> methodPredicate) {
    return getMethods().stream().map(Method::getDescriptor).anyMatch(methodPredicate);
  }

  public void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public boolean isEnum() {
    return typeDeclaration.isEnum();
  }

  public boolean isEnumOrSubclass() {
    return isEnum() || (getSuperTypeDescriptor() != null && getSuperTypeDescriptor().isEnum());
  }

  public boolean isOptimizedEnum() {
    return isOptimizedEnum;
  }

  public void setOptimizedEnum(boolean optimizedEnum) {
    checkState(isEnum());
    isOptimizedEnum = optimizedEnum;
  }

  public boolean isInterface() {
    return typeDeclaration.isInterface();
  }

  public boolean isClass() {
    return typeDeclaration.isClass();
  }

  public TypeDeclaration getOverlaidTypeDeclaration() {
    return typeDeclaration.getOverlaidTypeDeclaration();
  }

  public boolean isOverlayImplementation() {
    return typeDeclaration.getOverlaidTypeDeclaration() != null;
  }

  public TypeDeclaration getUnderlyingTypeDeclaration() {
    return isOverlayImplementation() ? getOverlaidTypeDeclaration() : getDeclaration();
  }

  public boolean isJsEnum() {
    return typeDeclaration.isJsEnum();
  }

  public boolean isJsFunctionInterface() {
    return typeDeclaration.isJsFunctionInterface();
  }

  public boolean isJsFunctionImplementation() {
    return typeDeclaration.isJsFunctionImplementation();
  }

  public List<Type> getTypes() {
    return types;
  }

  public void addType(Type type) {
    types.add(type);
  }

  public List<Member> getMembers() {
    return members;
  }

  public void addMember(Member member) {
    members.add(checkNotNull(member));
  }

  public void addMember(int position, Member member) {
    members.add(position, checkNotNull(member));
  }

  public void addMembers(Collection<? extends Member> newMembers) {
    members.addAll(newMembers);
  }

  public ImmutableList<Field> getFields() {
    return members.stream()
        .filter(Member::isField)
        .map(Field.class::cast)
        .collect(toImmutableList());
  }

  /**
   * Since enum fields are just tracked as static final fields in Type we want to be able to
   * distinguish enum fields from static fields created in the enum body.
   */
  public ImmutableList<Field> getEnumFields() {
    return getFields().stream().filter(Field::isEnumField).collect(toImmutableList());
  }

  public ImmutableList<Method> getMethods() {
    return members.stream()
        .filter(Member::isMethod)
        .map(Method.class::cast)
        .collect(toImmutableList());
  }

  public boolean hasInstanceInitializerBlocks() {
    return members.stream()
        .filter(Predicates.not(Member::isStatic))
        .anyMatch(Member::isInitializerBlock);
  }

  public void addInstanceInitializerBlock(Block instanceInitializer) {
    members.add(
        InitializerBlock.newBuilder()
            .setBlock(instanceInitializer)
            .setSourcePosition(instanceInitializer.getSourcePosition())
            .setDescriptor(getTypeDescriptor().getInitMethodDescriptor())
            .build());
  }

  public void addStaticInitializerBlock(Block staticInitializer) {
    members.add(
        InitializerBlock.newBuilder()
            .setBlock(staticInitializer)
            .setSourcePosition(staticInitializer.getSourcePosition())
            .setDescriptor(getTypeDescriptor().getClinitMethodDescriptor())
            .build());
  }

  public void addStaticInitializerBlock(int index, Block staticInitializer) {
    members.add(
        index,
        InitializerBlock.newBuilder()
            .setBlock(staticInitializer)
            .setSourcePosition(staticInitializer.getSourcePosition())
            .setDescriptor(getTypeDescriptor().getClinitMethodDescriptor())
            .build());
  }

  public void addLoadTimeStatement(Statement loadTimeStatement) {
    loadTimeStatements.add(loadTimeStatement);
  }

  public List<Statement> getLoadTimeStatements() {
    return loadTimeStatements;
  }

  public TypeDeclaration getEnclosingTypeDeclaration() {
    return typeDeclaration.getEnclosingTypeDeclaration();
  }

  public void setSuperTypeDescriptor(DeclaredTypeDescriptor superTypeDescriptor) {
    this.superTypeDescriptor = superTypeDescriptor;
  }

  public DeclaredTypeDescriptor getSuperTypeDescriptor() {
    return superTypeDescriptor;
  }

  public List<DeclaredTypeDescriptor> getSuperInterfaceTypeDescriptors() {
    return typeDeclaration.getInterfaceTypeDescriptors();
  }

  public Stream<DeclaredTypeDescriptor> getSuperTypesStream() {
    DeclaredTypeDescriptor superTypeDescriptor = this.superTypeDescriptor;
    if (isInterface()) {
      // Add java.lang.Object as an implicit supertype of interfaces.
      checkState(superTypeDescriptor == null);
      superTypeDescriptor = TypeDescriptors.get().javaLangObject;
    }
    return Stream.concat(
            getSuperInterfaceTypeDescriptors().stream(), Stream.of(superTypeDescriptor))
        .filter(Predicates.notNull());
  }

  public TypeDeclaration getDeclaration() {
    return typeDeclaration;
  }

  public ImmutableList<Field> getInstanceFields() {
    return members.stream()
        .filter(Member::isField)
        .filter(Predicates.not(Member::isStatic))
        .map(Field.class::cast)
        .collect(toImmutableList());
  }

  public ImmutableList<Member> getInstanceMembers() {
    return members.stream().filter(Predicates.not(Member::isStatic)).collect(toImmutableList());
  }

  public ImmutableList<Field> getStaticFields() {
    return members.stream()
        .filter(Member::isField)
        .filter(Member::isStatic)
        .map(Field.class::cast)
        .collect(toImmutableList());
  }

  public ImmutableList<InitializerBlock> getStaticInitializerBlocks() {
    return members.stream()
        .filter(Member::isStatic)
        .filter(Member::isInitializerBlock)
        .map(InitializerBlock.class::cast)
        .collect(toImmutableList());
  }

  public ImmutableList<InitializerBlock> getInstanceInitializerBlocks() {
    return members.stream()
        .filter(Predicates.not(Member::isStatic))
        .filter(Member::isInitializerBlock)
        .map(InitializerBlock.class::cast)
        .collect(toImmutableList());
  }

  public ImmutableList<Method> getConstructors() {
    return getMethods().stream().filter(Member::isConstructor).collect(toImmutableList());
  }

  @Nullable
  public Method getDefaultConstructor() {
    // TODO(b/215777271): This doesn't consider varags constructors as a default constructor.
    return getMethods().stream()
        .filter(m -> m.isConstructor() && m.getParameters().isEmpty())
        .findFirst()
        .orElse(null);
  }

  @Override
  public String getSimpleJsName() {
    return typeDeclaration.getSimpleJsName();
  }

  @Override
  public String getJsNamespace() {
    return typeDeclaration.getJsNamespace();
  }

  @Override
  public boolean isNative() {
    return typeDeclaration.isNative();
  }

  @Override
  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public String getReadableDescription() {
    return getDeclaration().getReadableDescription();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_Type.visit(processor, this);
  }

  /** Synthesizes a getter and a holder for a lazily initialized field. */
  public void synthesizeLazilyInitializedField(
      String fieldName, Expression initializationExpression, MethodDescriptor lazyFieldGetter) {
    TypeDescriptor expressionTypeDescriptor = initializationExpression.getTypeDescriptor();

    FieldDescriptor holderFieldDescriptor =
        getLazyFieldHolderFieldDescriptor(getTypeDescriptor(), expressionTypeDescriptor, fieldName);

    // Creates the member that will hold the value.
    addMember(
        Field.Builder.from(holderFieldDescriptor).setSourcePosition(SourcePosition.NONE).build());

    // Synthesizes the getter:
    // $get<fieldName>() {
    //   if (<fieldName> != null) {
    //     return <fieldName>;
    //   }
    //   <fieldName> = <initializationExpression>;
    //   return <fieldName>;
    // }
    addMember(
        Method.newBuilder()
            .setMethodDescriptor(lazyFieldGetter)
            .addStatements(
                IfStatement.newBuilder()
                    .setConditionExpression(
                        FieldAccess.Builder.from(holderFieldDescriptor)
                            .build()
                            .infixNotEqualsNull())
                    .setThenStatement(
                        ReturnStatement.newBuilder()
                            .setExpression(FieldAccess.Builder.from(holderFieldDescriptor).build())
                            .setSourcePosition(SourcePosition.NONE)
                            .build())
                    .setSourcePosition(SourcePosition.NONE)
                    .build(),
                BinaryExpression.Builder.asAssignmentTo(holderFieldDescriptor)
                    .setRightOperand(initializationExpression)
                    .build()
                    .makeStatement(SourcePosition.NONE),
                ReturnStatement.newBuilder()
                    .setExpression(FieldAccess.Builder.from(holderFieldDescriptor).build())
                    .setSourcePosition(SourcePosition.NONE)
                    .build())
            .setSourcePosition(SourcePosition.NONE)
            .build());
  }

  private static FieldDescriptor getLazyFieldHolderFieldDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      TypeDescriptor fieldTypeDescriptor,
      String name) {
    return FieldDescriptor.newBuilder()
        .setName(name)
        .setTypeDescriptor(fieldTypeDescriptor)
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setStatic(true)
        .setSynthetic(true)
        .build();
  }
}
