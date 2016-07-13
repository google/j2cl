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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.ast.annotations.Visitable;

/** Class for local variable and parameter. */
@Visitable
public class Variable extends Node {
  private String name;
  @Visitable TypeDescriptor typeDescriptor;
  private boolean isFinal;
  private boolean isParameter;
  private boolean isRaw;

  public Variable(
      String name,
      TypeDescriptor typeDescriptor,
      boolean isFinal,
      boolean isParameter,
      boolean isRaw) {
    setName(name);
    setTypeDescriptor(typeDescriptor);
    this.isFinal = isFinal;
    this.isParameter = isParameter;
    this.isRaw = isRaw;
  }

  public Variable(
      String name, TypeDescriptor typeDescriptor, boolean isFinal, boolean isParameter) {
    this(name, typeDescriptor, isFinal, isParameter, false);
  }

  public String getName() {
    return name;
  }

  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public boolean isParameter() {
    return isParameter;
  }

  /**
   * Returns whether this is a Raw variable. Raw variables are not aliased in the output and thus
   * can be used to represent JS native variables, for example, 'arguments'.
   */
  public boolean isRaw() {
    return isRaw;
  }

  public void setName(String name) {
    this.name = checkNotNull(name);
  }

  public void setTypeDescriptor(TypeDescriptor typeDescriptor) {
    this.typeDescriptor = checkNotNull(typeDescriptor);
  }

  public void setFinal(boolean isFinal) {
    this.isFinal = isFinal;
  }

  public void setParameter(boolean isParameter) {
    this.isParameter = isParameter;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Variable.visit(processor, this);
  }

  public Expression getReference() {
    return new VariableReference(this);
  }
}
