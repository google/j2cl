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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Number literal node. */
@Visitable
public class NumberLiteral extends Literal {
  public static NumberLiteral fromInt(int value) {
    return new NumberLiteral(PrimitiveTypes.INT, value);
  }

  public static NumberLiteral fromChar(int value) {
    return new NumberLiteral(PrimitiveTypes.CHAR, value);
  }

  public static NumberLiteral fromLong(long value) {
    return new NumberLiteral(PrimitiveTypes.LONG, value);
  }

  private final PrimitiveTypeDescriptor typeDescriptor;
  private final Number value;

  public NumberLiteral(PrimitiveTypeDescriptor typeDescriptor, Number value) {
    this.typeDescriptor = checkNotNull(typeDescriptor);
    this.value = coerceValue(typeDescriptor, value);
    // Make sure numberLiterals never store a Float value. Value is emitted directly by
    // ExpressionTranspiler float literals would have a shorter but less precise representation.
    checkState(!(this.value instanceof Float));
  }

  private Number coerceValue(PrimitiveTypeDescriptor typeDescriptor, Number value) {
    checkState(TypeDescriptors.isNumericPrimitive(typeDescriptor));
    if (TypeDescriptors.isPrimitiveByte(typeDescriptor)) {
      return value.byteValue();
    } else if (TypeDescriptors.isPrimitiveShort(typeDescriptor)) {
      return value.shortValue();
    } else if (TypeDescriptors.isPrimitiveChar(typeDescriptor)) {
      // Force narrowing then box to Integer.
      return Integer.valueOf((char) value.intValue());
    } else if (TypeDescriptors.isPrimitiveInt(typeDescriptor)) {
      return value.intValue();
    } else if (TypeDescriptors.isPrimitiveLong(typeDescriptor)) {
      return value.longValue();
    } else if (TypeDescriptors.isPrimitiveFloat(typeDescriptor)) {
      // Do not use floatValue() here, since J2cl does not honor 32-bit float semantics.
      return value.doubleValue();
    } else if (TypeDescriptors.isPrimitiveDouble(typeDescriptor)) {
      return value.doubleValue();
    }
    throw new InternalCompilerError("Not a numeric type: %s.", typeDescriptor);
  }

  public Number getValue() {
    return value;
  }

  @Override
  public Precedence getPrecedence() {
    // Positive number literals have the highest precedence and never need to be parenthesized, but
    // negative number can be seen as a prefix - on a positive number literal, so their precedence
    // needs to be PREFIX.
    return value.doubleValue() < 0 ? Precedence.PREFIX : Precedence.HIGHEST;
  }

  @Override
  public PrimitiveTypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public NumberLiteral clone() {
    // Number literals are value types do not need to actually clone.
    return this;
  }

  @Override
  public String getSourceText() {
    return value.toString();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_NumberLiteral.visit(processor, this);
  }
}
