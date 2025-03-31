/*
 * Copyright 2025 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;
import java.util.Objects;

// Do not mark ArrayConstant as @Visitable. Currently ArrayConstant is never present in the tree.
// Before making it @Visitable, we need to ensure that {@link #clone()} is implemented correctly or
// the class is unmodifiable.
/** Class for array literals whose values are all literals. */
public class ArrayConstant extends Literal {

  private final ArrayTypeDescriptor typeDescriptor;
  private final ImmutableList<Literal> valueExpressions;

  private ArrayConstant(
      ArrayTypeDescriptor typeDescriptor, ImmutableList<Literal> valueExpressions) {
    checkState(typeDescriptor.isArray());

    this.typeDescriptor = typeDescriptor;
    this.valueExpressions = valueExpressions;
  }

  @Override
  public ArrayTypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  public ImmutableList<Literal> getValueExpressions() {
    return valueExpressions;
  }

  @Override
  public String getSourceText() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ArrayConstant clone() {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ArrayConstant)) {
      return false;
    }
    ArrayConstant other = (ArrayConstant) o;
    return typeDescriptor.equals(other.typeDescriptor)
        && valueExpressions.equals(other.valueExpressions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(typeDescriptor, valueExpressions);
  }

  public static Builder newBuilder() {
    return new ArrayConstant.Builder();
  }

  /** Builder for {@link ArrayConstant}. */
  public static final class Builder {
    private ArrayTypeDescriptor typeDescriptor = null;
    private ImmutableList<Literal> valueExpressions = ImmutableList.of();

    @CanIgnoreReturnValue
    public Builder setTypeDescriptor(ArrayTypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setValueExpressions(List<Literal> valueExpressions) {
      this.valueExpressions = ImmutableList.copyOf(valueExpressions);
      return this;
    }

    public ArrayConstant build() {
      return new ArrayConstant(typeDescriptor, valueExpressions);
    }

    private Builder() {}
  }
}
