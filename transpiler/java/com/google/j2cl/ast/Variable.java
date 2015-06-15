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

/**
 * Base class for any variable, including field, local variable, and parameter.
 */
public abstract class Variable extends Node {
  protected String name;
  protected TypeReference type;
  protected boolean isFinal;
  @Visitable protected Expression initializer;

  public Variable(String name, TypeReference type, boolean isFinal, Expression initializer) {
    this.name = name;
    this.type = type;
    this.isFinal = isFinal;
    this.initializer = initializer;
  }

  public String getName() {
    return name;
  }

  public TypeReference getType() {
    return type;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public Expression getInitializer() {
    return initializer;
  }

  public boolean hasInitializer() {
    return initializer != null;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setType(TypeReference type) {
    this.type = type;
  }

  public void setFinal(boolean isFinal) {
    this.isFinal = isFinal;
  }

  public void setInitializer(Expression initializer) {
    this.initializer = initializer;
  }
}
