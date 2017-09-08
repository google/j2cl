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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.SourcePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Method declaration. */
@Visitable
public class Method extends Member implements HasJsNameInfo, HasParameters, HasMethodDescriptor {
  @Visitable MethodDescriptor methodDescriptor;
  @Visitable List<Variable> parameters = new ArrayList<>();
  @Visitable Block body;
  private boolean isOverride;
  private String jsDocDescription;

  private static TypeDescriptor getEnclosingTypeDescriptor(Method method) {
    return method.getDescriptor().getEnclosingTypeDescriptor();
  }

  private Method(
      SourcePosition sourcePosition,
      MethodDescriptor methodDescriptor,
      List<Variable> parameters,
      Block body,
      boolean isOverride,
      String jsDocDescription) {
    super(sourcePosition);
    this.methodDescriptor = checkNotNull(methodDescriptor);
    this.parameters.addAll(checkNotNull(parameters));
    this.isOverride = isOverride;
    this.jsDocDescription = jsDocDescription;
    this.body = checkNotNull(body);
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
  public Variable getJsVarargsParameter() {
    if (methodDescriptor.isJsMethodVarargs()) {
      return getVarargsParameter();
    }
    return null;
  }

  public Variable getVarargsParameter() {
    if (methodDescriptor.isVarargs()) {
      return Iterables.getLast(getParameters());
    }
    return null;
  }

  @Override
  public String getQualifiedBinaryName() {
    return getDescriptor().getQualifiedBinaryName();
  }

  public Block getBody() {
    return body;
  }

  @Override
  public boolean isConstructor() {
    return methodDescriptor.isConstructor();
  }
  @Override
  public boolean isMethod() {
    return true;
  }

  public boolean isBridge() {
    return methodDescriptor.isBridge();
  }

  public boolean isSynthetic() {
    return methodDescriptor.isSynthetic();
  }

  @Override
  public boolean isAbstract() {
    return methodDescriptor.isAbstract();
  }

  public boolean isOverride() {
    return this.isOverride;
  }

  public void setOverride(boolean isOverride) {
    this.isOverride = isOverride;
  }

  public boolean isFinal() {
    return getDescriptor().isFinal();
  }

  @Override
  public boolean isStatic() {
    return methodDescriptor.isStatic();
  }

  public String getJsDocDescription() {
    return jsDocDescription;
  }

  @Override
  public String getSimpleJsName() {
    return methodDescriptor.getSimpleJsName();
  }

  @Override
  public String getJsNamespace() {
    return methodDescriptor.getJsNamespace();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public String getReadableDescription() {
    // TODO(b/36493405): once a parameter abstraction is implemented in MethodDescriptor that
    // stores parameter names, this method should just delegate to it.
    String parameterString =
        getParameters().stream().map(this::getParameterReadableDescription).collect(joining(", "));

    if (isConstructor()) {
      return J2clUtils.format(
          "%s(%s)",
          getEnclosingTypeDescriptor(Method.this).getReadableDescription(), parameterString);
    }
    return J2clUtils.format(
        "%s %s.%s(%s)",
        getDescriptor().getReturnTypeDescriptor().getReadableDescription(),
        getEnclosingTypeDescriptor(Method.this).getReadableDescription(),
        getDescriptor().getName(),
        parameterString);
  }

  private String getParameterReadableDescription(Variable parameter) {
    if (parameter == getVarargsParameter()) {
      return J2clUtils.format(
          "%s... %s",
          parameter.getTypeDescriptor().getComponentTypeDescriptor().getReadableDescription(),
          parameter.getName());
    }
    return J2clUtils.format(
        "%s %s", parameter.getTypeDescriptor().getReadableDescription(), parameter.getName());
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Method.visit(processor, this);
  }

  /** Returns true if the method is locally empty (allows calls to super constructor). */
  public boolean isEmpty() {
    // TODO(rluble): to be completely correct the parameters of the implicit supercall need to
    // be examined.
    List<Statement> statements = getBody().getStatements();
    return statements.isEmpty()
        || (statements.size() == 1 && isConstructor() && AstUtils.hasSuperCall(this));
  }

  /**
   * A Builder for easily and correctly creating modified versions of methods.
   *
   * <p>Takes care of the busy work of keeping parameter list and method descriptor parameter type
   * list in sync.
   */
  public static class Builder {

    private MethodDescriptor methodDescriptor;
    private List<Variable> parameters = new ArrayList<>();
    private List<Statement> statements = new ArrayList<>();
    private boolean isOverride;
    private String jsDocDescription;
    private SourcePosition bodySourcePosition;
    private SourcePosition sourcePosition;

    public static Builder from(Method method) {
      Builder builder = new Builder();
      builder.methodDescriptor = method.getDescriptor();
      builder.parameters = Lists.newArrayList(method.getParameters());
      builder.statements = Lists.newArrayList(method.getBody().getStatements());
      builder.isOverride = method.isOverride();
      builder.jsDocDescription = method.getJsDocDescription();
      builder.bodySourcePosition = method.getBody().getSourcePosition();
      builder.sourcePosition = method.getSourcePosition();
      return builder;
    }

    public Builder addParameters(int index, Variable... parameters) {
      return addParameters(index, Arrays.asList(parameters));
    }

    public Builder addParameters(int index, Collection<Variable> newParameters) {
      parameters.addAll(index, newParameters);
      methodDescriptor =
          MethodDescriptor.Builder.from(methodDescriptor)
              .addParameterTypeDescriptors(
                  index,
                  newParameters
                      .stream()
                      .map(Variable::getTypeDescriptor)
                      .collect(toImmutableList()))
              .build();
      return this;
    }

    public Builder addStatements(Statement... statements) {
      Collections.addAll(this.statements, statements);
      return this;
    }

    public Builder addStatements(List<Statement> statements) {
      this.statements.addAll(statements);
      return this;
    }

    public Builder addStatement(int index, Statement statement) {
      this.statements.add(index, statement);
      return this;
    }

    public Builder setParameters(Variable... parameters) {
      return setParameters(Arrays.asList(parameters));
    }

    public Builder setParameters(Collection<Variable> parameters) {
      this.parameters = new ArrayList<>(parameters);
      checkState(parameters.size() == methodDescriptor.getParameterDescriptors().size());
      return this;
    }

    public Builder setMethodDescriptor(MethodDescriptor methodDescriptor) {
      this.methodDescriptor = methodDescriptor;
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

    public Builder setOverride(boolean isOverride) {
      this.isOverride = isOverride;
      return this;
    }

    public Builder setJsDocDescription(String jsDocDescription) {
      this.jsDocDescription = jsDocDescription;
      return this;
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Method build() {
      if (bodySourcePosition == null) {
        bodySourcePosition = sourcePosition;
      }
      Block body = new Block(bodySourcePosition, statements);
      checkState(parameters.size() == methodDescriptor.getParameterDescriptors().size());

      Set<TypeDescriptor> typeParametersTypeDescriptors =
          new LinkedHashSet<>(methodDescriptor.getTypeParameterTypeDescriptors());
      for (Variable parameter : parameters) {
        // Collect type variables that have been introduced by new parameters and are
        // not already type parameters of the method nor of the enclosing class.
        typeParametersTypeDescriptors.addAll(
            parameter
                .getTypeDescriptor()
                .getAllTypeVariables()
                .stream()
                .filter(
                    typeVariable ->
                        !methodDescriptor
                            .getEnclosingTypeDescriptor()
                            .getTypeArgumentDescriptors()
                            .contains(typeVariable))
                .collect(Collectors.toList()));
      }
      return new Method(
          sourcePosition,
          MethodDescriptor.Builder.from(methodDescriptor)
              .updateParameterTypeDescriptors(
                  parameters.stream().map(Variable::getTypeDescriptor).collect(toImmutableList()))
              .setTypeParameterTypeDescriptors(typeParametersTypeDescriptors)
              .build(),
          parameters,
          body,
          isOverride,
          jsDocDescription);
    }
  }
}
