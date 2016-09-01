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
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.ast.annotations.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for variable declaration expression.
 */
@Visitable
public class VariableDeclarationExpression extends Expression {
  @Visitable List<VariableDeclarationFragment> fragments = new ArrayList<>();

  public VariableDeclarationExpression(VariableDeclarationFragment... fragments) {
    this(Arrays.asList(fragments));
  }

  public VariableDeclarationExpression(List<VariableDeclarationFragment> fragments) {
    this.fragments.addAll(checkNotNull(fragments));
    checkArgument(!fragments.isEmpty());
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
    return new VariableDeclarationExpression(AstUtils.clone(fragments));
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_VariableDeclarationExpression.visit(processor, this);
  }
}
