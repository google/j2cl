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

import com.google.common.collect.Lists;
import com.google.j2cl.ast.processors.Context;
import com.google.j2cl.ast.processors.Visitable;

import java.util.ArrayList;
import java.util.List;

/**
 * Method declaration.
 */
@Visitable
@Context
public class Method extends Node {
  @Visitable MethodDescriptor methodDescriptor;
  @Visitable List<Variable> parameters = new ArrayList<>();
  @Visitable Block body;
  private boolean isAbstract;
  private boolean isOverride;
  private String jsDocDescription;
  private boolean isFinal;

  public Method(MethodDescriptor methodDescriptor, List<Variable> parameters, Block body) {
    this(methodDescriptor, parameters, body, false, false, false, null);
  }

  public Method(
      MethodDescriptor methodDescriptor,
      List<Variable> parameters,
      Block body,
      boolean isAbstract,
      boolean isOverride,
      boolean isFinal,
      String jsDocDescription) {
    this.methodDescriptor = checkNotNull(methodDescriptor);
    this.parameters.addAll(checkNotNull(parameters));
    this.isAbstract = isAbstract;
    this.isOverride = isOverride;
    this.isFinal = isFinal;
    this.jsDocDescription = jsDocDescription;
    this.body = checkNotNull(body);
    if (jsDocDescription == null && isConstructor()) {
      this.jsDocDescription = "Initializes instance fields for a particular Java constructor.";
    }
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

  public boolean isConstructor() {
    return methodDescriptor.isConstructor();
  }

  public boolean isNative() {
    return methodDescriptor.isNative();
  }

  public boolean isAbstract() {
    return this.isAbstract;
  }

  public void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  public boolean isOverride() {
    return this.isOverride;
  }

  public void setOverride(boolean isOverride) {
    this.isOverride = isOverride;
  }

  public boolean isFinal() {
    return this.isFinal;
  }

  public void setFinal(boolean isFinal) {
    this.isFinal = isFinal;
  }

  public String getJsDocDescription() {
    return jsDocDescription;
  }
  
  public boolean isPrimaryConstructor() {
    if (isConstructor()) {
      if (getDescriptor().getEnclosingClassTypeDescriptor().subclassesJsConstructorClass()) {
        return !AstUtils.hasThisCall(this);
      }
    }
    return false;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Method.visit(processor, this);
  }

  /**
   * A Builder for easily and correctly creating modified versions of methods.
   *
   * <p>Takes care of the busy work of keeping parameter list and method descriptor parameter type
   * list in sync.
   */
  public static class Builder {

    private MethodDescriptor originalMethodDescriptor;
    private List<Variable> originalParameterVariables = new ArrayList<>();
    private List<Variable> addedParameterVariables = new ArrayList<>();
    private List<TypeDescriptor> addedParameterTypeDescriptors = new ArrayList<>();
    private List<Statement> statements = new ArrayList<>();
    private boolean isAbstract;
    private boolean isOverride;
    private String jsDocDescription;
    private boolean isFinal;

    public static Builder from(Method method) {
      Builder builder = new Builder();
      builder.originalMethodDescriptor = method.getDescriptor();
      builder.originalParameterVariables = Lists.newArrayList(method.getParameters());
      builder.statements = Lists.newArrayList(method.getBody().getStatements());
      builder.isAbstract = method.isAbstract();
      builder.isOverride = method.isOverride();
      builder.jsDocDescription = method.getJsDocDescription();
      builder.isFinal = method.isFinal();
      builder.isFinal = method.isFinal();
      return builder;
    }

    public Builder parameter(Variable parameterVariable, TypeDescriptor parameterTypeDescriptor) {
      addedParameterVariables.add(parameterVariable);
      addedParameterTypeDescriptors.add(parameterTypeDescriptor);
      return this;
    }

    public Builder statement(Statement statement) {
      statements.add(statement);
      return this;
    }

    public Builder statement(int index, Statement statement) {
      statements.add(index, statement);
      return this;
    }

    public Builder clearStatements() {
      statements.clear();
      return this;
    }

    public Builder removeStatement(int index) {
      statements.remove(index);
      return this;
    }

    public Builder enclosingClass(TypeDescriptor enclosingClassTypeDescriptor) {
      this.originalMethodDescriptor =
          MethodDescriptor.Builder.from(originalMethodDescriptor)
              .enclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
              .build();
      return this;
    }

    public Builder isOverride(boolean isOverride) {
      this.isOverride = isOverride;
      return this;
    }

    public Builder jsDocDescription(String jsDocDescription) {
      this.jsDocDescription = jsDocDescription;
      return this;
    }

    public Method build() {
      List<Variable> finalParameters = new ArrayList<>();
      finalParameters.addAll(originalParameterVariables);
      finalParameters.addAll(addedParameterVariables);

      MethodDescriptor finalMethodDescriptor =
          MethodDescriptors.createModifiedCopy(
              originalMethodDescriptor, addedParameterTypeDescriptors);

      return new Method(
          finalMethodDescriptor,
          finalParameters,
          new Block(statements),
          isAbstract,
          isOverride,
          isFinal,
          jsDocDescription);
    }
  }
}
