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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Class for new instance expression.
 */
@Visitable
public class NewInstance extends Invocation {
  @Visitable @Nullable Expression qualifier;
  @Visitable MethodDescriptor constructorMethodDescriptor;
  @Visitable List<Expression> arguments = new ArrayList<>();

  private NewInstance(
      Expression qualifier,
      MethodDescriptor constructorMethodDescriptor,
      List<Expression> arguments) {
    this.constructorMethodDescriptor = checkNotNull(constructorMethodDescriptor);
    this.qualifier = qualifier;
    this.arguments.addAll(checkNotNull(arguments));
  }

  @Override
  public Expression getQualifier() {
    return qualifier;
  }

  @Override
  public MethodDescriptor getTarget() {
    return constructorMethodDescriptor;
  }

  @Override
  public List<Expression> getArguments() {
    return arguments;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return constructorMethodDescriptor.getEnclosingClassTypeDescriptor();
  }

  @Override
  public NewInstance clone() {
    return new NewInstance(
        qualifier != null ? qualifier.clone() : null,
        constructorMethodDescriptor,
        AstUtils.clone(arguments));
  }

  @Override
  Builder createBuilder() {
    return new Builder(this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_NewInstance.visit(processor, this);
  }

  /**
   * A Builder for easily and correctly creating modified versions of new instance calls.
   *
   * <p>Takes care of the busy work of keeping argument list and method descriptor parameter types
   * list in sync.
   */
  public static class Builder extends Invocation.Builder {
    public static Builder from(NewInstance newInstance) {
      return new Builder(newInstance);
    }

    public static Builder from(MethodDescriptor constructorDescriptor) {
      Builder builder = new Builder();
      builder.setMethodDescriptor(constructorDescriptor);
      return builder;
    }

    @Override
    public Builder setQualifier(Expression qualifier) {
      super.setQualifier(qualifier);
      return this;
    }

    @Override
    public Builder setArguments(List<Expression> arguments) {
      super.setArguments(arguments);
      return this;
    }

    @Override
    public NewInstance build() {
      return (NewInstance) super.build();
    }

    @Override
    protected NewInstance doCreateInvocation(
        Expression qualifierExpression,
        MethodDescriptor methodDescriptor,
        List<Expression> arguments) {
      return new NewInstance(qualifierExpression, methodDescriptor, arguments);
    }
    
    private Builder(NewInstance newInstance) {
      super(newInstance);
    }

    private Builder() {}
  }
}
