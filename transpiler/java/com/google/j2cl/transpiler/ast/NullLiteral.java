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

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Null literal node. */
@Visitable
public class NullLiteral extends Literal {

  static NullLiteral get(TypeDescriptor typeDescriptor) {
    return new NullLiteral(typeDescriptor.toNullable());
  }

  private NullLiteral(TypeDescriptor typeDescriptor) {
    this.typeDescriptor = typeDescriptor;
  }

  private final TypeDescriptor typeDescriptor;

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public String getSourceText() {
    return "null";
  }

  @Override
  public NullLiteral clone() {
    // Null literals are value types do not need to actually clone.
    return this;
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_NullLiteral.visit(processor, this);
  }
}
