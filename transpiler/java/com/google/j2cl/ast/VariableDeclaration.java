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
 * Class for variable declaration.
 */
@Visitable
public class VariableDeclaration extends Statement {
  @Visitable Variable variable;
  @Visitable Expression initializer;

  public VariableDeclaration(Variable variable, Expression initializer) {
    this.variable = variable;
    this.initializer = initializer;
  }

  public Variable getVariable() {
    return variable;
  }

  public Expression getInitializer() {
    return initializer;
  }

  public void setVariable(Variable variable) {
    this.variable = variable;
  }

  public void setInitializer(Expression initializer) {
    this.initializer = initializer;
  }

  @Override
  public VariableDeclaration accept(Processor processor) {
    return Visitor_VariableDeclaration.visit(processor, this);
  }
}
