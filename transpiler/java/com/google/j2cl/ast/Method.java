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

import com.google.j2cl.ast.processors.Context;
import com.google.j2cl.ast.processors.Visitable;

import java.util.ArrayList;
import java.util.List;

/**
 * Method declaration.
 */
@Visitable @Context
public class Method extends Node {
  @Visitable MethodDescriptor methodDescriptor;
  @Visitable List<Variable> parameters = new ArrayList<>();
  @Visitable Block body;

  public Method(MethodDescriptor methodDescriptor, List<Variable> parameters, Block body) {
    this.methodDescriptor = methodDescriptor;
    this.parameters.addAll(parameters);
    this.body = body;
  }

  public MethodDescriptor getDescriptor() {
    return methodDescriptor;
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
    return methodDescriptor.isConstructor();
  }

  public void setBody(Block body) {
    this.body = body;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Method.visit(processor, this);
  }
}
