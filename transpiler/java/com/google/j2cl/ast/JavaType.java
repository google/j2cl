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

import java.util.ArrayList;
import java.util.List;

/**
 * A node that represents a Java Type declaration in the compilation unit.
 */
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

  @Visitable TypeReference enclosingType;
  @Visitable TypeReference superType;
  @Visitable List<TypeReference> superInterfaces = new ArrayList<>();
  @Visitable TypeReference selfReference;
  @Visitable List<Field> fields = new ArrayList<>();
  @Visitable List<Method> methods = new ArrayList<>();

  /**
   * instance initialization statements, including the initializations in instance field
   * declarations and in non-static initialization blocks.
   * TODO: to implement a concrete Node type for initializations.
   */
  @Visitable List<Node> instanceInitializers = new ArrayList<>();

  /**
   * static initialization statements, including the initializations in static field declarations
   * and in static initialization blocks.
   * TODO: to implement a concrete Node type for initializations.
   */
  @Visitable List<Node> staticInitializers = new ArrayList<>();

  public JavaType(Kind kind, Visibility visibility, TypeReference selfReference) {
    this.kind = kind;
    this.visibility = visibility;
    this.selfReference = selfReference;
  }

  public Kind getKind() {
    return kind;
  }

  public void setKind(Kind kind) {
    this.kind = kind;
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
    this.fields.addAll(fields);
  }

  public List<Method> getMethods() {
    return methods;
  }

  public void addMethod(Method method) {
    methods.add(method);
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public void setVisibility(Visibility visibility) {
    this.visibility = visibility;
  }

  public List<Node> getInstanceInitializer() {
    return instanceInitializers;
  }

  public List<Node> getStaticInitializer() {
    return staticInitializers;
  }

  public TypeReference getEnclosingType() {
    return enclosingType;
  }

  public void setEnclosingType(TypeReference enclosingType) {
    this.enclosingType = enclosingType;
  }

  public TypeReference getSuperType() {
    return superType;
  }

  public void setSuperType(TypeReference superType) {
    this.superType = superType;
  }

  public List<TypeReference> getSuperInterfaces() {
    return superInterfaces;
  }

  public void setSuperInterfaces(List<TypeReference> superInterfaces) {
    this.superInterfaces = superInterfaces;
  }

  public TypeReference getSelfReference() {
    return selfReference;
  }

  public void setSelfReference(TypeReference selfReference) {
    this.selfReference = selfReference;
  }

  JavaType accept(Visitor visitor) {
    return VisitorJavaType.visit(visitor, this);
  }
}
