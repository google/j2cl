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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/** Class for an inline (lambda) function expression. */
@Visitable
public class FunctionExpression extends Expression implements MethodLike {
  // The visitors traverse the @Visitable members of the class in the order they appear.
  // The parameter declarations need to be traversed before the body.
  @Visitable final List<Variable> parameters;
  @Visitable Block body;
  @Visitable TypeDescriptor typeDescriptor;
  private final SourcePosition sourcePosition;
  private final boolean isJsAsync;

  private FunctionExpression(
      SourcePosition sourcePosition,
      TypeDescriptor typeDescriptor,
      List<Variable> parameters,
      Block body,
      boolean isJsAsync) {
    this.parameters = checkNotNull(parameters);
    this.body = checkNotNull(body);
    this.typeDescriptor = typeDescriptor.toNonNullable();
    this.sourcePosition = checkNotNull(sourcePosition);
    this.isJsAsync = isJsAsync;
    checkNotNull(typeDescriptor.getFunctionalInterface());
  }

  @Override
  public MethodDescriptor getDescriptor() {
    return typeDescriptor.getFunctionalInterface().getSingleAbstractMethodDescriptor();
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

  @Override
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
            if (isDeclaredWithinFunctionExpression(
                receiverReference.getTypeDescriptor().getTypeDeclaration())) {
              // This is a reference to this of an anonymous or local class declared inside a
              // lambda. This reference is not capturing an enclosing instance.
              return;
            }
            hasEnclosingInstanceReferences[0] = true;
          }

          /**
           * Returns {@code true} if the type object declaring {@code typeDeclaration} was seen
           * enclosing the traversal.
           *
           * <p>Note this relies on the fact the traversal starts at the function expression and
           * does not see the types that enclose it, only types that are defined in its body.
           */
          private boolean isDeclaredWithinFunctionExpression(TypeDeclaration typeDeclaration) {
            Predicate<Object> matchesTypeDeclaration =
                n -> n instanceof Type && ((Type) n).getDeclaration() == typeDeclaration;
            return getParent(matchesTypeDeclaration) != null;
          }
        });

    return hasEnclosingInstanceReferences[0];
  }

  public boolean isJsVarargs() {
    return getDescriptor().isJsMethodVarargs();
  }

  public boolean isJsAsync() {
    return isJsAsync;
  }

  @Override
  public Precedence getPrecedence() {
    return Precedence.FUNCTION;
  }

  @Override
  public boolean canBeNull() {
    return false;
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
        .setJsAsync(isJsAsync)
        .setSourcePosition(sourcePosition)
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
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
    private boolean isJsAsync;
    private SourcePosition sourcePosition;

    public static Builder from(FunctionExpression expression) {
      return new Builder()
          .setTypeDescriptor(expression.getTypeDescriptor())
          .setParameters(expression.getParameters())
          .setStatements(expression.getBody().getStatements())
          .setJsAsync(expression.isJsAsync)
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

    public Builder setJsAsync(boolean isJsAsync) {
      this.isJsAsync = isJsAsync;
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
          Block.newBuilder().setSourcePosition(sourcePosition).setStatements(statements).build(),
          isJsAsync);
    }
  }
}
