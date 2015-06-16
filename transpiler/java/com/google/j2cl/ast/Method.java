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
  // TODO: implement concrete type for method body.
  private UnknownNode body;

  public Method(MethodReference selfReference, List<Variable> parameters, UnknownNode body) {
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

  public UnknownNode getBody() {
    return body;
  }

  public void addParameter(Variable parameter) {
    parameters.add(parameter);
  }

  public void setBody(UnknownNode body) {
    this.body = body;
  }

  Method accept(Visitor visitor) {
    return VisitorMethod.visit(visitor, this);
  }
}
