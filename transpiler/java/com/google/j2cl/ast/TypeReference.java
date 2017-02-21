/*
 * Copyright 2016 Google Inc.
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

/**
 * Refers to a Java type or a constructor in Javascript.
 */
@Visitable
public class TypeReference extends Expression {
  @Visitable TypeDescriptor typeDescriptor;

  public TypeReference(TypeDescriptor typeDescriptor) {
    this.typeDescriptor = checkNotNull(typeDescriptor);
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return TypeDescriptors.NATIVE_FUNCTION;
  }

  @Override
  public boolean isIdempotent() {
    return true;
  }

  @Override
  public TypeReference clone() {
    return new TypeReference(typeDescriptor);
  }

  public TypeDescriptor getReferencedTypeDescriptor() {
    return typeDescriptor;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_TypeReference.visit(processor, this);
  }
}
