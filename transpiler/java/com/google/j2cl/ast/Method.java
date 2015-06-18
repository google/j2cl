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
 * Method declaration.
 */
public class Method extends Node {
  private final MethodReference selfReference;
  @Visitable List<Variable> parameters = new ArrayList<>();
  @Visitable Block body;

  public Method(MethodReference selfReference, List<Variable> parameters, Block body) {
    this.selfReference = selfReference;
    this.parameters.addAll(parameters);
    this.body = body;
  }

  public MethodReference getSelfReference() {
    return selfReference;
  }

  public List<Variable> getParameters() {
    return parameters;
  }

  public Block getBody() {
    return body;
  }

  public void addParameter(Variable parameter) {
    parameters.add(parameter);
  }

  public boolean isConstructor() {
    return selfReference.isConstructor();
  }

  public void setBody(Block body) {
    this.body = body;
  }

  @Override
  Method accept(Visitor visitor) {
    return VisitorMethod.visit(visitor, this);
  }
}
