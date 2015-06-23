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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Class for new instance expression.
 */
@Visitable
public class NewInstance extends Expression {

  @Visitable @Nullable Expression qualifier;

  @Visitable MethodReference constructorRef;

  @Visitable List<Expression> arguments = new ArrayList<>();

  public NewInstance(
      Expression qualifier, MethodReference constructorRef, List<Expression> arguments) {
    this.qualifier = qualifier;
    this.constructorRef = constructorRef;
    this.arguments.addAll(arguments);
  }

  public Expression getQualifier() {
    return qualifier;
  }

  public MethodReference getConstructorRef() {
    return constructorRef;
  }

  public List<Expression> getArguments() {
    return arguments;
  }

  public void setQualifier(Expression qualifier) {
    this.qualifier = qualifier;
  }

  public void setConstructorRef(MethodReference constructorRef) {
    this.constructorRef = constructorRef;
  }

  @Override
  public NewInstance accept(Processor processor) {
    return Visitor_NewInstance.visit(processor, this);
  }
}
