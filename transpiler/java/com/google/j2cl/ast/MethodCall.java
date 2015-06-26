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
 * Class for method call expression.
 */
@Visitable
public class MethodCall extends Expression implements MemberReference {
  @Visitable @Nullable Expression qualifier;
  @Visitable MethodReference target;
  @Visitable List<Expression> arguments = new ArrayList<>();

  public MethodCall(Expression qualifier, MethodReference target, List<Expression> arguments) {
    this.qualifier = qualifier;
    this.target = target;
    this.arguments.addAll(arguments);
  }

  @Override
  public Expression getQualifier() {
    return this.qualifier;
  }

  @Override
  public MethodReference getTarget() {
    return this.target;
  }

  public List<Expression> getArguments() {
    return this.arguments;
  }

  public void setQualifier(Expression qualifier) {
    this.qualifier = qualifier;
  }

  public void setTarget(MethodReference target) {
    this.target = target;
  }

  @Override
  public MethodCall accept(Processor processor) {
    return Visitor_MethodCall.visit(processor, this);
  }
}
