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
import javax.annotation.Nullable;

/**
 * Represents a part of a variable declaration.
 *
 * <p>From the statement int i = 0; the fragment would represent i = 0;
 */
@Visitable
public class VariableDeclarationFragment extends Node
    implements Cloneable<VariableDeclarationFragment> {
  @Visitable Variable variable;
  @Visitable @Nullable Expression initializer;

  public VariableDeclarationFragment(Variable variable, Expression initializer) {
    this.variable = checkNotNull(variable);
    this.initializer = initializer;
  }

  /** Returns true if the variable declaration needs to be JsDoc annotated on output. */
  public boolean needsTypeDeclaration() {
    return initializer == null;
  }

  public Variable getVariable() {
    return variable;
  }

  public Expression getInitializer() {
    return initializer;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_VariableDeclarationFragment.visit(processor, this);
  }

  @Override
  public VariableDeclarationFragment clone() {
    // TODO(rluble): Move variables to the block and have a variable reference in this class
    // instead. The proposed representation is better in the sense that variables references will
    // only be present in children nodes (not sibling nodes).
    return new VariableDeclarationFragment(
        // DO NOT clone the variable here as it would make all the references be out of sync
        // pointing to a different variable instance. Variables are replaced explicitly by using
        // AstUtils.replaceVariables.
        variable, AstUtils.clone(initializer));
  }
}
