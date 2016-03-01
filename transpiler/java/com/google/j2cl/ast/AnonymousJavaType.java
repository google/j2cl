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
 * A node that represents an anonymous class declaration.
 *
 * <p>Besides the fields in a regular JavaType, an AnonymousJavaType has some extra fields. An
 * anonymous class is declared by a NewInstance expression, thus it may have implicit constructor
 * parameters (provided by NewInstance arguments) and qualifier for super call (provided by
 * NewInstance qualifier).
 */
public class AnonymousJavaType extends JavaType {
  public AnonymousJavaType(Kind kind, Visibility visibility, TypeDescriptor typeDescriptor) {
    super(kind, visibility, typeDescriptor);
  }

  /**
   * Parameter types of default constructor. It is used to synthesize default constructor of
   * anonymous class.
   */
  private List<TypeDescriptor> constructorParameterTypeDescriptors = new ArrayList<>();

  /**
   * Parameter types of the super class's default constructor. Used to synthesize the call to the
   * default constructor of the super class, if one exists.
   */
  private List<TypeDescriptor> superConstructorParameterTypeDescriptors = new ArrayList<>();

  /**
   * Qualifier of NewInstance, which is actually the qualifier of the super call and is a
   * reference to what will be the enclosing instance for this class's super type.
   */
  private Expression superCallQualifier;

  public List<TypeDescriptor> getConstructorParameterTypeDescriptors() {
    return constructorParameterTypeDescriptors;
  }

  public List<TypeDescriptor> getSuperConstructorParameterTypeDescriptors() {
    return superConstructorParameterTypeDescriptors;
  }

  public void addConstructorParameterTypeDescriptors(
      List<TypeDescriptor> constructorParameterTypeDescriptors) {
    this.constructorParameterTypeDescriptors.addAll(constructorParameterTypeDescriptors);
  }

  public void addSuperConstructorParameterTypeDescriptors(
      List<TypeDescriptor> superConstructorParameterTypeDescriptors) {
    this.superConstructorParameterTypeDescriptors.addAll(superConstructorParameterTypeDescriptors);
  }

  public Expression getSuperCallQualifier() {
    return superCallQualifier;
  }

  public void setSuperCallQualifier(Expression qualifier) {
    this.superCallQualifier = qualifier;
  }
}
