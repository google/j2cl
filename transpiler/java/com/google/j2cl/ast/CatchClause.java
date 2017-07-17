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
import java.util.Collections;

/** Class for catch clause. */
@Visitable
public class CatchClause extends Node implements Cloneable<CatchClause> {
  // The visitors traverse the @Visitable members of the class in the order they appear.
  // The variable definition needs to be traversed before the body of the the catch clause.
  @Visitable Variable exceptionVar;
  @Visitable Block body;

  public CatchClause(Variable exceptionVar, Block body) {
    this.body = checkNotNull(body);
    this.exceptionVar = checkNotNull(exceptionVar);
  }

  public Block getBody() {
    return body;
  }

  public Variable getExceptionVar() {
    return exceptionVar;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_CatchClause.visit(processor, this);
  }

  @Override
  public CatchClause clone() {
    Variable clonedExceptionVariable = exceptionVar.clone();
    Block clonedBody =
        AstUtils.replaceVariables(
            Collections.singletonList(exceptionVar),
            Collections.singletonList(clonedExceptionVariable),
            body.clone());

    return new CatchClause(clonedExceptionVariable, clonedBody);
  }
}
