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

/** Number literal node. */
@Visitable
public class NumberLiteral extends Literal {
  private final TypeDescriptor typeDescriptor;
  private final Number value;

  public NumberLiteral(TypeDescriptor typeDescriptor, Number value) {
    this.typeDescriptor = checkNotNull(typeDescriptor);
    this.value = value;
  }

  public Number getValue() {
    return value;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public NumberLiteral clone() {
    // Number literals are value types do not need to actually clone.
    return this;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_NumberLiteral.visit(processor, this);
  }
}
