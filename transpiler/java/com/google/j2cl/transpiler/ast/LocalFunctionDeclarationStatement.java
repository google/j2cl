/*
 * Copyright 2025 Google Inc.
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
import static com.google.common.base.Preconditions.checkState;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Class for local function definitions. */
@Visitable
public class LocalFunctionDeclarationStatement extends Statement implements MethodLike {
  @Visitable MethodDescriptor methodDescriptor;
  @Visitable List<Variable> parameters;
  @Visitable Block body;

  private LocalFunctionDeclarationStatement(
      SourcePosition sourcePosition,
      MethodDescriptor methodDescriptor,
      List<Variable> parameters,
      Block body) {
    super(sourcePosition);
    this.parameters = checkNotNull(parameters);
    this.body = checkNotNull(body);
    this.methodDescriptor = checkNotNull(methodDescriptor);
  }

  @Override
  public MethodDescriptor getDescriptor() {
    return methodDescriptor;
  }

  @Override
  public List<Variable> getParameters() {
    return parameters;
  }

  @Override
  public Block getBody() {
    return body;
  }

  @Override
  public String getReadableDescription() {
    return "<local function " + getDescriptor().getName() + ">";
  }

  @Override
  public LocalFunctionDeclarationStatement clone() {
    var clonedParameters = AstUtils.clone(parameters);
    Block clonedBody = AstUtils.replaceDeclarations(parameters, clonedParameters, body.clone());

    return LocalFunctionDeclarationStatement.newBuilder()
        .setMethodDescriptor(methodDescriptor)
        .setParameters(clonedParameters)
        .setBody(clonedBody)
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_LocalFunctionDeclarationStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for LocalFunctionDeclarationStatement. */
  public static class Builder {
    private List<Variable> parameters = new ArrayList<>();
    private Block body;
    private MethodDescriptor methodDescriptor;
    private SourcePosition sourcePosition;

    public static Builder from(LocalFunctionDeclarationStatement expression) {
      return new Builder()
          .setMethodDescriptor(expression.getDescriptor())
          .setParameters(expression.getParameters())
          .setBody(expression.getBody())
          .setSourcePosition(expression.getSourcePosition());
    }

    @CanIgnoreReturnValue
    public Builder setBody(Block body) {
      this.body = body;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setParameters(Variable... parameters) {
      return setParameters(Arrays.asList(parameters));
    }

    @CanIgnoreReturnValue
    public Builder setParameters(List<Variable> parameters) {
      this.parameters = new ArrayList<>(parameters);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setMethodDescriptor(MethodDescriptor methodDescriptor) {
      this.methodDescriptor = methodDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public LocalFunctionDeclarationStatement build() {
      checkNotNull(methodDescriptor);
      checkState(methodDescriptor.isLocalFunction());

      return new LocalFunctionDeclarationStatement(
          sourcePosition, methodDescriptor, parameters, body);
    }
  }
}
