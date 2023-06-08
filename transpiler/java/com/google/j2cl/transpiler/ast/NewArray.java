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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
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
  /**
   * An optional initializer for the array being constructed.
   *
   * <p>If present it should statisfy one of: 1. Be an {@link ArrayLiteral} that has the same type
   * as the array being constructed. 2. Be a {@link FunctionExpression} or a {@link MethodReference}
   * whose SAM type accepts a single int param (index) and returns the component type of the array
   * being constructed.
   */
  @Nullable @Visitable Expression initializer;

  private NewArray(
      ArrayTypeDescriptor typeDescriptor,
      List<Expression> dimensionExpressions,
      @Nullable Expression initializer) {
    checkArgument(typeDescriptor.getDimensions() == dimensionExpressions.size());
    checkArgument(
        initializer == null || isValidInitializer(initializer, typeDescriptor),
        "Invalid initializer:\n%s",
        initializer);

    this.typeDescriptor = checkNotNull(typeDescriptor.toNonNullable());
    this.dimensionExpressions.addAll(checkNotNull(dimensionExpressions));
    this.initializer = initializer;
  }

  @Nullable
  public Expression getInitializer() {
    return initializer;
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
        .setInitializer(AstUtils.clone(initializer))
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_NewArray.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for NewArray. */
  public static class Builder {
    public static Builder from(NewArray newArray) {
      return newBuilder()
          .setInitializer(newArray.getInitializer())
          .setDimensionExpressions(newArray.getDimensionExpressions())
          .setTypeDescriptor(newArray.getTypeDescriptor());
    }

    private ArrayTypeDescriptor typeDescriptor;
    private Expression initializer;
    private List<Expression> dimensionExpressions;

    @CanIgnoreReturnValue
    public Builder setTypeDescriptor(ArrayTypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setInitializer(Expression initializer) {
      this.initializer = initializer;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setDimensionExpressions(List<Expression> dimensionExpressions) {
      this.dimensionExpressions = dimensionExpressions;
      return this;
    }

    public NewArray build() {
      return new NewArray(typeDescriptor, dimensionExpressions, initializer);
    }
  }

  private static boolean isValidInitializer(
      Expression initializer, ArrayTypeDescriptor typeDescriptor) {
    if (initializer instanceof ArrayLiteral) {
      return ((ArrayLiteral) initializer).getTypeDescriptor().hasSameRawType(typeDescriptor);
    }

    DeclaredTypeDescriptor functionalInterface =
        checkNotNull(
            initializer.getTypeDescriptor().getFunctionalInterface(),
            "Array initializer type should be a functional interface");
    checkState(functionalInterface.isJsFunctionInterface());
    MethodDescriptor methodDescriptor =
        checkNotNull(functionalInterface.getSingleAbstractMethodDescriptor());
    // A valid initializer SAM type:
    //  - should have a return type that matches the component type of the array
    //  - should only accept a single int parameter (the index)
    return methodDescriptor
            .getReturnTypeDescriptor()
            .hasSameRawType(typeDescriptor.getComponentTypeDescriptor())
        && methodDescriptor.getParameterDescriptors().size() == 1
        && TypeDescriptors.isPrimitiveInt(
            methodDescriptor.getParameterDescriptors().get(0).getTypeDescriptor());
  }
}
