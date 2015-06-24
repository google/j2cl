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

import com.google.j2cl.ast.processors.Visitable;

import javax.annotation.Nullable;

/**
 * Class for this reference.
 */
@Visitable
public class ThisReference extends Expression {
  @Visitable @Nullable RegularTypeReference typeRef;

  public ThisReference(RegularTypeReference typeRef) {
    this.typeRef = typeRef;
  }

  public RegularTypeReference getTypeRef() {
    return typeRef;
  }

  public void setTypeRef(RegularTypeReference qualifier) {
    this.typeRef = qualifier;
  }

  @Override
  public ThisReference accept(Processor processor) {
    return Visitor_ThisReference.visit(processor, this);
  }
}
