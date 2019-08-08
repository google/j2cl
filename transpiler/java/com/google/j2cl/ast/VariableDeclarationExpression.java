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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.processors.common.Processor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Class for variable declaration expression.
 */
@Visitable
public class VariableDeclarationExpression extends Expression {
  @Visitable List<VariableDeclarationFragment> fragments;

  private VariableDeclarationExpression(List<VariableDeclarationFragment> fragments) {
    checkArgument(!fragments.isEmpty());
    this.fragments = new ArrayList<>(fragments);
  }

  public List<VariableDeclarationFragment> getFragments() {
    return fragments;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return fragments.get(0).getVariable().getTypeDescriptor();
  }

  @Override
  public VariableDeclarationExpression clone() {
    return VariableDeclarationExpression.newBuilder()
        .setVariableDeclarationFragments(AstUtils.clone(fragments))
        .build();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_VariableDeclarationExpression.visit(processor, this);
  }
  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for VariableDeclarationExpression. */
  public static class Builder {
    private List<VariableDeclarationFragment> fragments = new ArrayList<>();

    public static Builder from(VariableDeclarationExpression variableDeclarationExpression) {
      return newBuilder()
          .setVariableDeclarationFragments(variableDeclarationExpression.getFragments());
    }

    public Builder setVariableDeclarationFragments(
        List<VariableDeclarationFragment> variableDeclarationFragments) {
      this.fragments = new ArrayList<>(variableDeclarationFragments);
      return this;
    }

    public Builder addVariableDeclaration(Variable variable, Expression initializer) {
      fragments.add(
          VariableDeclarationFragment.newBuilder()
              .setVariable(variable)
              .setInitializer(initializer)
              .build());
      return this;
    }

    public Builder addVariableDeclarations(Variable... variables) {
      return addVariableDeclarations(Arrays.asList(variables));
    }

    public Builder addVariableDeclarations(Collection<Variable> variables) {
      return addVariableDeclarationFragments(
          variables.stream()
              .map(v -> VariableDeclarationFragment.newBuilder().setVariable(v).build())
              .collect(toImmutableList()));
    }

    public Builder addVariableDeclarationFragments(
        VariableDeclarationFragment... variableDeclarationFragments) {
      return addVariableDeclarationFragments(Arrays.asList(variableDeclarationFragments));
    }

    public Builder addVariableDeclarationFragments(
        Collection<VariableDeclarationFragment> variableDeclarationFragment) {
      fragments.addAll(variableDeclarationFragment);
      return this;
    }

    public VariableDeclarationExpression build() {
      return new VariableDeclarationExpression(fragments);
    }
  }
}
