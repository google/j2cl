/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

/** Class for an inline (lambda) function expression. */
@Visitable
public class FunctionExpression extends Expression implements MethodLike {
  // The visitors traverse the @Visitable members of the class in the order they appear.
  // The parameter declarations need to be traversed before the body.
  @Visitable final List<Variable> parameters;
  @Visitable Block body;
  private final TypeDescriptor typeDescriptor;
  private final SourcePosition sourcePosition;

  private FunctionExpression(
      SourcePosition sourcePosition,
      TypeDescriptor typeDescriptor,
      List<Variable> parameters,
      Block body) {
    this.parameters = checkNotNull(parameters);
    this.body = checkNotNull(body);
    this.typeDescriptor = typeDescriptor.toNonNullable();
    this.sourcePosition = checkNotNull(sourcePosition);
    checkNotNull(typeDescriptor.getFunctionalInterface());
  }

  @Override
  public MethodDescriptor getDescriptor() {
    // TODO(b/208830469): When a function expression is a JsFunction we use the
    // JsFunctionMethodDescriptor that might differ slightly from the single abstract method
    // from the interface. When there is a difference it is because the interface method has
    // type parameters, which are not expressible in Closure, and are removed from the
    // JsFunction method descriptor.
    return typeDescriptor.isJsFunctionInterface()
        ? typeDescriptor.getFunctionalInterface().getJsFunctionMethodDescriptor()
        : typeDescriptor.getFunctionalInterface().getSingleAbstractMethodDescriptor();
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public List<Variable> getParameters() {
    return parameters;
  }

  @Override
  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Nullable
  @Override
  public Variable getJsVarargsParameter() {
    if (isJsVarargs()) {
      return Iterables.getLast(getParameters());
    }
    return null;
  }

  public Block getBody() {
    return body;
  }

  /** Returns true if this functional expression references the enclosing instance. */
  public boolean isCapturingEnclosingInstance() {
    boolean[] hasEnclosingInstanceReferences = new boolean[1];
    accept(
        new AbstractVisitor() {
          @Override
          public void exitThisOrSuperReference(ThisOrSuperReference receiverReference) {
            hasEnclosingInstanceReferences[0] = true;
          }
        });
    return hasEnclosingInstanceReferences[0];
  }

  public boolean isJsVarargs() {
    return getDescriptor().isJsMethodVarargs();
  }

  @Override
  public Precedence getPrecedence() {
    return Precedence.FUNCTION;
  }

  @Override
  public String getReadableDescription() {
    return "<lambda>";
  }

  @Override
  public FunctionExpression clone() {
    List<Variable> clonedParameters = AstUtils.clone(parameters);
    Block clonedBody = AstUtils.replaceDeclarations(parameters, clonedParameters, body.clone());

    return FunctionExpression.newBuilder()
        .setTypeDescriptor(typeDescriptor)
        .setParameters(clonedParameters)
        .setStatements(clonedBody.getStatements())
        .setSourcePosition(sourcePosition)
        .build();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_FunctionExpression.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for FunctionExpression. */
  public static class Builder {
    private List<Variable> parameters = new ArrayList<>();
    private List<Statement> statements = new ArrayList<>();
    private TypeDescriptor typeDescriptor;
    private SourcePosition sourcePosition;

    public static Builder from(FunctionExpression expression) {
      return new Builder()
          .setTypeDescriptor(expression.getTypeDescriptor())
          .setParameters(expression.getParameters())
          .setStatements(expression.getBody().getStatements())
          .setSourcePosition(expression.getSourcePosition());
    }

    public Builder setStatements(List<Statement> statements) {
      this.statements = new ArrayList<>(statements);
      return this;
    }

    public Builder setStatements(Statement... statements) {
      this.statements = Arrays.asList(statements);
      return this;
    }

    public Builder setParameters(Variable... parameters) {
      return setParameters(Arrays.asList(parameters));
    }

    public Builder setParameters(List<Variable> parameters) {
      this.parameters = new ArrayList<>(parameters);
      return this;
    }

    public Builder setTypeDescriptor(TypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public FunctionExpression build() {
      return new FunctionExpression(
          sourcePosition,
          typeDescriptor,
          parameters,
          Block.newBuilder().setSourcePosition(sourcePosition).setStatements(statements).build());
    }
  }
}
