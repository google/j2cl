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

import com.google.j2cl.ast.processors.Visitable;

/**
 * Class for local variable and parameter.
 */
@Visitable
public class Variable extends Node {
  private String name;
  @Visitable TypeReference type;
  private boolean isFinal;
  private boolean isParameter;

  public Variable(String name, TypeReference type, boolean isFinal, boolean isParameter) {
    this.name = name;
    this.type = type;
    this.isFinal = isFinal;
    this.isParameter = isParameter;
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

  public boolean isParameter() {
    return isParameter;
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

  public void setParameter(boolean isParameter) {
    this.isParameter = isParameter;
  }

  @Override
  Variable accept(Visitor visitor) {
    return Visitor_Variable.visit(visitor, this);
  }
}
