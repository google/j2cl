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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.processors.Context;
import com.google.j2cl.ast.processors.Visitable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A node that represents a Java Type declaration in the compilation unit.
 */
@Visitable
@Context
public class JavaType extends Node {
  /**
   * Describes the kind of the Java type.
   */
  public enum Kind {
    CLASS,
    INTERFACE,
    ENUM
  }

  private Kind kind;
  private Visibility visibility;
  private boolean isStatic;
  private boolean isLocal;

  @Visitable @Nullable TypeDescriptor enclosingTypeDescriptor;

  @Visitable @Nullable TypeDescriptor superTypeDescriptor;

  @Visitable List<TypeDescriptor> superInterfaceTypeDescriptors = new ArrayList<>();

  @Visitable TypeDescriptor typeDescriptor;

  @Visitable List<Field> fields = new ArrayList<>();

  @Visitable List<Method> methods = new ArrayList<>();

  @Visitable List<Block> instanceInitializerBlocks = new ArrayList<>();

  @Visitable List<Block> staticInitializerBlocks = new ArrayList<>();

  public JavaType(Kind kind, Visibility visibility, TypeDescriptor typeDescriptor) {
    this.kind = kind;
    this.visibility = visibility;
    this.typeDescriptor = typeDescriptor;
  }

  public Kind getKind() {
    return kind;
  }

  public void setKind(Kind kind) {
    this.kind = kind;
  }

  public boolean isLocal() {
    return isLocal;
  }

  public void setLocal(boolean isLocal) {
    this.isLocal = isLocal;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public void setStatic(boolean isStatic) {
    this.isStatic = isStatic;
  }

  public boolean isEnum() {
    return this.kind == Kind.ENUM;
  }

  public boolean isInterface() {
    return this.kind == Kind.INTERFACE;
  }

  public List<Field> getFields() {
    return fields;
  }

  public void addField(Field field) {
    fields.add(field);
  }

  public void addFields(List<Field> fields) {
    Preconditions.checkNotNull(fields);
    this.fields.addAll(fields);
  }

  public List<Method> getMethods() {
    return methods;
  }

  public void addMethod(Method method) {
    methods.add(method);
  }

  public void addMethods(List<Method> methods) {
    this.methods.addAll(methods);
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public void setVisibility(Visibility visibility) {
    this.visibility = visibility;
  }

  public List<Block> getInstanceInitializerBlocks() {
    return instanceInitializerBlocks;
  }

  public void addInstanceInitializerBlock(Block instanceInitializer) {
    this.instanceInitializerBlocks.add(instanceInitializer);
  }

  public List<Block> getStaticInitializerBlocks() {
    return staticInitializerBlocks;
  }

  public void addStaticInitializerBlock(Block staticInitializer) {
    this.staticInitializerBlocks.add(staticInitializer);
  }

  public TypeDescriptor getEnclosingTypeDescriptor() {
    return enclosingTypeDescriptor;
  }

  public void setEnclosingTypeDescriptor(TypeDescriptor enclosingTypeDescriptor) {
    this.enclosingTypeDescriptor = enclosingTypeDescriptor;
  }

  public TypeDescriptor getSuperTypeDescriptor() {
    return superTypeDescriptor;
  }

  public void setSuperTypeDescriptor(TypeDescriptor superTypeDescriptor) {
    this.superTypeDescriptor = superTypeDescriptor;
  }

  public List<TypeDescriptor> getSuperInterfaceTypeDescriptors() {
    return superInterfaceTypeDescriptors;
  }

  public void addSuperInterfaceDescriptor(TypeDescriptor superInterfaceTypeDescriptor) {
    this.superInterfaceTypeDescriptors.add(superInterfaceTypeDescriptor);
  }

  public TypeDescriptor getDescriptor() {
    return typeDescriptor;
  }

  public void setDescriptor(TypeDescriptor typeDescriptor) {
    this.typeDescriptor = typeDescriptor;
  }

  public List<Field> getInstanceFields() {
    return Lists.newArrayList(
        Iterables.filter(
            getFields(),
            new Predicate<Field>() {
              @Override
              public boolean apply(Field field) {
                return !field.getDescriptor().isStatic();
              }
            }));
  }

  public List<Field> getStaticFields() {
    return Lists.newArrayList(
        Iterables.filter(
            getFields(),
            new Predicate<Field>() {
              @Override
              public boolean apply(Field field) {
                return field.getDescriptor().isStatic();
              }
            }));
  }

  public List<Method> getConstructors() {
    return Lists.newArrayList(
        Iterables.filter(
            getMethods(),
            new Predicate<Method>() {
              @Override
              public boolean apply(Method method) {
                return method.isConstructor();
              }
            }));
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_JavaType.visit(processor, this);
  }
}
