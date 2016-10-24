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
import static java.util.stream.Collectors.toCollection;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.j2cl.ast.annotations.Context;
import com.google.j2cl.ast.annotations.Visitable;
import java.util.ArrayList;
import java.util.List;

/** A node that represents a Java Type declaration in the compilation unit. */
@Visitable
@Context
public class Type extends Node {
  /** Describes the kind of the Java type. */
  public enum Kind {
    CLASS,
    INTERFACE,
    ENUM
  }

  private Kind kind;
  private Visibility visibility;
  private boolean isStatic;
  private boolean isAbstract;
  private boolean isAnonymous;
  @Visitable TypeDescriptor typeDescriptor;
  @Visitable List<Member> members = new ArrayList<>();

  // Used to store the original native type for a synthesized JsOverlyImpl type.
  private TypeDescriptor overlayTypeDescriptor;

  public Type(Kind kind, Visibility visibility, TypeDescriptor typeDescriptor) {
    this.kind = kind;
    this.visibility = visibility;
    this.typeDescriptor = typeDescriptor;
  }

  public Kind getKind() {
    return kind;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public boolean containsMethod(String mangledName) {
    for (Method method : getMethods()) {
      MethodDescriptor methodDescriptor = method.getDescriptor();
      if (ManglingNameUtils.getMangledName(methodDescriptor).equals(mangledName)) {
        return true;
      }
    }
    return false;
  }

  public boolean containsNonJsNativeMethods() {
    for (Method method : getMethods()) {
      if (method.isNative()
          && !method.getDescriptor().isJsPropertyGetter()
          && !method.getDescriptor().isJsPropertySetter()
          && !method.getDescriptor().isJsMethod()) {
        return true;
      }
    }
    return false;
  }

  public boolean containsDefaultMethods() {
    if (!isInterface()) {
      return false;
    }
    for (Method method : getMethods()) {
      if (method.getDescriptor().isDefault()) {
        return true;
      }
    }
    return false;
  }

  public void setStatic(boolean isStatic) {
    this.isStatic = isStatic;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  public void setAnonymous(boolean isAnonymous) {
    this.isAnonymous = isAnonymous;
  }

  public boolean isAnonymous() {
    return isAnonymous;
  }

  public boolean isEnum() {
    return this.kind == Kind.ENUM;
  }

  public boolean isInterface() {
    return this.kind == Kind.INTERFACE;
  }

  public boolean isClass() {
    return this.kind == Kind.ENUM || this.kind == Kind.CLASS;
  }

  public TypeDescriptor getNativeTypeDescriptor() {
    return this.overlayTypeDescriptor;
  }

  public void setNativeTypeDescriptor(TypeDescriptor nativeTypeDescriptor) {
    this.overlayTypeDescriptor = nativeTypeDescriptor;
  }

  public boolean isJsOverlayImplementation() {
    return getNativeTypeDescriptor() != null;
  }

  public List<Member> getMembers() {
    return members;
  }

  public Iterable<Field> getFields() {
    return AstUtils.filterFields(members);
  }

  public void addField(Field field) {
    members.add(field);
  }

  public void addFields(List<Field> fields) {
    this.members.addAll(checkNotNull(fields));
  }

  public void addFields(int position, List<Field> fields) {
    this.members.addAll(position, checkNotNull(fields));
  }

  /**
   * Since enum fields are just tracked as static final fields in Type we want to be able to
   * distinguish enum fields from static fields created in the enum body.
   */
  public List<Field> getEnumFields() {
    checkArgument(this.kind == Kind.ENUM);
    Iterable<Field> enumFields = Iterables.filter(getFields(), Field::isEnumField);
    return Lists.newArrayList(enumFields);
  }

  public Iterable<Method> getMethods() {
    return AstUtils.filterMethods(members);
  }

  public void addMethod(Method method) {
    members.add(method);
  }

  public void addMethod(int index, Method method) {
    checkArgument(index >= 0 && index <= members.size());
    members.add(index, method);
  }

  public void addMethods(Iterable<Method> methods) {
    Iterables.addAll(members, methods);
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public boolean hasInstanceInitializerBlocks() {
    return !Iterables.isEmpty(AstUtils.filterInitializerBlocks(getInstanceMembers()));
  }

  public void addInstanceInitializerBlock(Block instanceInitializer) {
    members.add(new InitializerBlock(instanceInitializer, false));
  }

  public boolean hasStaticInitializerBlocks() {
    return !Iterables.isEmpty(AstUtils.filterInitializerBlocks(getStaticMembers()));
  }

  public void addStaticInitializerBlock(Block staticInitializer) {
    members.add(new InitializerBlock(staticInitializer, true));
  }

  public TypeDescriptor getEnclosingTypeDescriptor() {
    return typeDescriptor.getEnclosingTypeDescriptor();
  }

  public TypeDescriptor getSuperTypeDescriptor() {
    return typeDescriptor.getSuperTypeDescriptor();
  }

  public List<TypeDescriptor> getSuperInterfaceTypeDescriptors() {
    return typeDescriptor.getInterfaceTypeDescriptors();
  }

  public TypeDescriptor getDescriptor() {
    return typeDescriptor;
  }

  public Iterable<Field> getInstanceFields() {
    return AstUtils.filterFields(getInstanceMembers());
  }

  public Iterable<Member> getInstanceMembers() {
    return Iterables.filter(members, member -> !member.isStatic());
  }

  public Iterable<Field> getStaticFields() {
    return AstUtils.filterFields(getStaticMembers());
  }

  public Iterable<Member> getStaticMembers() {
    return Iterables.filter(members, Member::isStatic);
  }

  public List<Method> getConstructors() {
    return Streams.stream(getMethods())
        .filter(Method::isConstructor)
        .collect(toCollection(ArrayList::new));
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Type.visit(processor, this);
  }
}
