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
import java.util.Arrays;
import java.util.List;

/** Block Statement. */
@Visitable
public class Block extends Statement {
  @Visitable List<Statement> statements = new ArrayList<>();

  public Block(Block fromBlock) {
    this(fromBlock.getStatements());
    setSourcePosition(fromBlock.getSourcePosition());
  }

  public Block(Statement... statements) {
    this(Arrays.asList(statements));
  }

  public Block(List<Statement> statements) {
    this.statements.addAll(checkNotNull(statements));
  }

  public Block() {}

  public List<Statement> getStatements() {
    return statements;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Block.visit(processor, this);
  }
}
