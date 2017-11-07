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

import com.google.j2cl.ast.annotations.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for array literal expressions.
 */
@Visitable
public class ArrayLiteral extends Expression {
  private final ArrayTypeDescriptor typeDescriptor;
  @Visitable List<Expression> valueExpressions = new ArrayList<>();

  public ArrayLiteral(ArrayTypeDescriptor typeDescriptor, Expression... valueExpressions) {
    this(typeDescriptor, Arrays.asList(valueExpressions));
  }

  public ArrayLiteral(ArrayTypeDescriptor typeDescriptor, List<Expression> valueExpressions) {
    checkState(typeDescriptor.isArray());

    this.typeDescriptor = checkNotNull(typeDescriptor);
    this.valueExpressions.addAll(checkNotNull(valueExpressions));
  }

  public List<Expression> getValueExpressions() {
    return valueExpressions;
  }

  @Override
  public ArrayTypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public ArrayLiteral clone() {
    return new ArrayLiteral(typeDescriptor, AstUtils.clone(valueExpressions));
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_ArrayLiteral.visit(processor, this);
  }
}
