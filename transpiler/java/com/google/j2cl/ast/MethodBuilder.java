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

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * A Builder for easily and correctly creating modified versions of methods.
 *
 * <p>Takes care of the busy work of keeping parameter list and method descriptor parameter type
 * list in sync.
 */
public class MethodBuilder {

  private MethodDescriptor originalMethodDescriptor;
  private List<Variable> originalParameterVariables = new ArrayList<>();
  private List<Variable> addedParameterVariables = new ArrayList<>();
  private List<TypeDescriptor> addedParameterTypeDescriptors = new ArrayList<>();
  private List<Statement> statements = new ArrayList<>();
  private boolean isAbstract;
  private boolean isOverride;
  private String jsDocDescription;
  private boolean isFinal;

  public static MethodBuilder from(Method method) {
    MethodBuilder builder = new MethodBuilder();
    builder.originalMethodDescriptor = method.getDescriptor();
    builder.originalParameterVariables = Lists.newArrayList(method.getParameters());
    builder.statements = Lists.newArrayList(method.getBody().getStatements());
    builder.isAbstract = method.isAbstract();
    builder.isOverride = method.isOverride();
    builder.jsDocDescription = method.getJsDocDescription();
    builder.isFinal = method.isFinal();
    return builder;
  }

  public MethodBuilder parameter(
      Variable parameterVariable, TypeDescriptor parameterTypeDescriptor) {
    addedParameterVariables.add(parameterVariable);
    addedParameterTypeDescriptors.add(parameterTypeDescriptor);
    return this;
  }

  public MethodBuilder statement(Statement statement) {
    statements.add(statement);
    return this;
  }

  public MethodBuilder statement(int index, Statement statement) {
    statements.add(index, statement);
    return this;
  }

  public MethodBuilder clearStatements() {
    statements.clear();
    return this;
  }

  public MethodBuilder removeStatement(int index) {
    statements.remove(index);
    return this;
  }

  public MethodBuilder enclosingClass(TypeDescriptor enclosingClassTypeDescriptor) {
    this.originalMethodDescriptor =
        MethodDescriptorBuilder.from(originalMethodDescriptor)
            .enclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
            .build();
    return this;
  }

  public MethodBuilder isOverride(boolean isOverride) {
    this.isOverride = isOverride;
    return this;
  }

  public MethodBuilder jsDocDescription(String jsDocDescription) {
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
