/*
 * Copyright 2018 Google Inc.
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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Type literal node. */
@Visitable
public class TypeLiteral extends Literal implements HasSourcePosition {

  private final TypeDescriptor referencedTypeDescriptor;
  private final SourcePosition sourcePosition;

  public TypeLiteral(SourcePosition sourcePosition, TypeDescriptor referencedTypeDescriptor) {
    this.referencedTypeDescriptor = checkNotNull(referencedTypeDescriptor);
    this.sourcePosition = checkNotNull(sourcePosition);
  }

  public TypeDescriptor getReferencedTypeDescriptor() {
    return referencedTypeDescriptor;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return TypeDescriptors.get().javaLangClass.toNonNullable();
  }

  @Override
  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public String getSourceText() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypeLiteral clone() {
    // Type literals are value types do not need to actually clone.
    return this;
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_TypeLiteral.visit(processor, this);
  }
}
