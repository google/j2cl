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
package com.google.j2cl.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.annotations.Context;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.processors.common.Processor;
import com.google.j2cl.common.SourcePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** A node that represents a Java Type declaration in the compilation unit. */
@Visitable
@Context
public class Type extends Node implements HasSourcePosition, HasJsNameInfo, HasReadableDescription {
  private final Visibility visibility;
  private boolean isStatic;
  private TypeDeclaration typeDeclaration;
  @Visitable List<Member> members = new ArrayList<>();
  private final SourcePosition sourcePosition;
  private DeclaredTypeDescriptor superTypeDescriptor;

  public Type(
      SourcePosition sourcePosition, Visibility visibility, TypeDeclaration typeDeclaration) {
    this.sourcePosition = checkNotNull(sourcePosition);
    checkArgument(
        typeDeclaration.isInterface() || typeDeclaration.isClass() || typeDeclaration.isEnum());
    this.visibility = visibility;
    this.typeDeclaration = typeDeclaration;
    this.superTypeDescriptor = typeDeclaration.getSuperTypeDescriptor();
  }

  /**
   * Returns the TypeDescriptor for this Type parametrized by the type variables from the
   * declaration.
   */
  public DeclaredTypeDescriptor getTypeDescriptor() {
    return getDeclaration().toUnparameterizedTypeDescriptor();
  }

  public Kind getKind() {
    return typeDeclaration.getKind();
  }

  public boolean isStatic() {
    return isStatic;
  }

  public boolean containsMethod(String mangledName) {
    return getMethods().stream()
        .anyMatch(method -> method.getDescriptor().getMangledName().equals(mangledName));
  }

  public boolean containsNonJsNativeMethods() {
    return getMethods().stream()
        .filter(Method::isNative)
        .anyMatch(method -> !method.getDescriptor().isJsMember());
  }

  public void setStatic(boolean isStatic) {
    this.isStatic = isStatic;
  }

  public boolean isAbstract() {
    return typeDeclaration.isAbstract();
  }

  public boolean isEnum() {
    return typeDeclaration.isEnum();
  }

  public boolean isEnumOrSubclass() {
    return isEnum() || (getSuperTypeDescriptor() != null && getSuperTypeDescriptor().isEnum());
  }

  public boolean isInterface() {
    return typeDeclaration.isInterface();
  }

  public boolean isClass() {
    return typeDeclaration.isClass();
  }

  public void setSuperTypeDescriptor(DeclaredTypeDescriptor superTypeDescriptor) {
    this.superTypeDescriptor = superTypeDescriptor;
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

  public List<Member> getMembers() {
    return members;
  }

  public void setMembers(List<Member> members) {
    this.members = members;
  }

  public ImmutableList<Field> getFields() {
    return members.stream()
        .filter(Member::isField)
        .map(Field.class::cast)
        .collect(ImmutableList.toImmutableList());
  }

  public void addField(Field field) {
    members.add(field);
  }

  public void addField(int position, Field field) {
    members.add(position, checkNotNull(field));
  }

  public void addFields(Collection<Field> fields) {
    members.addAll(checkNotNull(fields));
  }

  /**
   * Since enum fields are just tracked as static final fields in Type we want to be able to
   * distinguish enum fields from static fields created in the enum body.
   */
  public ImmutableList<Field> getEnumFields() {
    return getFields().stream().filter(Field::isEnumField).collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<Method> getMethods() {
    return members.stream()
        .filter(Member::isMethod)
        .map(Method.class::cast)
        .collect(ImmutableList.toImmutableList());
  }

  public void addMethod(Method method) {
    checkArgument(!method.isConstructor() || (!isInterface() && !isOverlayImplementation()));
    members.add(method);
  }

  public void addMethod(int index, Method method) {
    checkArgument(index >= 0 && index <= members.size());
    members.add(index, method);
  }

  public void addMethods(Collection<Method> methods) {
    members.addAll(methods);
  }

  public Visibility getVisibility() {
    return visibility;
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

  public boolean hasStaticInitializerBlocks() {
    return members.stream().filter(Member::isStatic).anyMatch(Member::isInitializerBlock);
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

  public TypeDeclaration getEnclosingTypeDeclaration() {
    return typeDeclaration.getEnclosingTypeDeclaration();
  }

  public DeclaredTypeDescriptor getSuperTypeDescriptor() {
    return superTypeDescriptor;
  }

  public List<DeclaredTypeDescriptor> getSuperInterfaceTypeDescriptors() {
    return typeDeclaration.getInterfaceTypeDescriptors();
  }

  public TypeDeclaration getDeclaration() {
    return typeDeclaration;
  }

  public ImmutableList<Field> getInstanceFields() {
    return members.stream()
        .filter(Member::isField)
        .filter(Predicates.not(Member::isStatic))
        .map(Field.class::cast)
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<Member> getInstanceMembers() {
    return members.stream()
        .filter(Predicates.not(Member::isStatic))
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<Field> getStaticFields() {
    return members.stream()
        .filter(Member::isField)
        .filter(Member::isStatic)
        .map(Field.class::cast)
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<InitializerBlock> getStaticInitializerBlocks() {
    return members.stream()
        .filter(Member::isStatic)
        .filter(Member::isInitializerBlock)
        .map(InitializerBlock.class::cast)
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<InitializerBlock> getInstanceInitializerBlocks() {
    return members.stream()
        .filter(Predicates.not(Member::isStatic))
        .filter(Member::isInitializerBlock)
        .map(InitializerBlock.class::cast)
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<Method> getConstructors() {
    return getMethods().stream()
        .filter(Member::isConstructor)
        .collect(ImmutableList.toImmutableList());
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
  public Node accept(Processor processor) {
    return Visitor_Type.visit(processor, this);
  }
}
