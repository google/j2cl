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
import com.google.j2cl.ast.processors.common.Processor;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Class for new array expression.
 */
@Visitable
public class NewArray extends Expression {
  private final ArrayTypeDescriptor typeDescriptor;
  @Visitable List<Expression> dimensionExpressions = new ArrayList<>();
  @Nullable @Visitable ArrayLiteral arrayLiteral;

  private NewArray(
      ArrayTypeDescriptor typeDescriptor,
      List<Expression> dimensionExpressions,
      ArrayLiteral arrayLiteral) {
    this.typeDescriptor = checkNotNull(typeDescriptor);
    this.dimensionExpressions.addAll(checkNotNull(dimensionExpressions));
    this.arrayLiteral = arrayLiteral;
    checkArgument(typeDescriptor.getDimensions() == dimensionExpressions.size());
    checkArgument(
        arrayLiteral == null || arrayLiteral.getTypeDescriptor().hasSameRawType(typeDescriptor));
  }

  public ArrayLiteral getArrayLiteral() {
    return arrayLiteral;
  }

  public List<Expression> getDimensionExpressions() {
    return dimensionExpressions;
  }

  public TypeDescriptor getLeafTypeDescriptor() {
    return typeDescriptor.getLeafTypeDescriptor();
  }

  @Override
  public ArrayTypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public Precedence getPrecedence() {
    // Treated exactly as new, which is modeled as a member access.
    return Precedence.MEMBER_ACCESS;
  }

  @Override
  public NewArray clone() {
    return NewArray.newBuilder()
        .setTypeDescriptor(typeDescriptor)
        .setDimensionExpressions(AstUtils.clone(dimensionExpressions))
        .setArrayLiteral(AstUtils.clone(arrayLiteral))
        .build();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_NewArray.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for NewArray. */
  public static class Builder {

    private ArrayTypeDescriptor typeDescriptor;
    private ArrayLiteral arrayLiteral;
    private List<Expression> dimensionExpressions;

    public Builder setTypeDescriptor(ArrayTypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    public Builder setArrayLiteral(ArrayLiteral arrayLiteral) {
      this.arrayLiteral = arrayLiteral;
      return this;
    }

    public Builder setDimensionExpressions(List<Expression> dimensionExpressions) {
      this.dimensionExpressions = dimensionExpressions;
      return this;
    }

    public NewArray build() {
      return new NewArray(typeDescriptor, dimensionExpressions, arrayLiteral);
    }
  }

}
