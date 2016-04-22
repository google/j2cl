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

/**
 * Null literal node.
 */
@Visitable
public class NullLiteral extends Expression {

  public static final NullLiteral NULL = new NullLiteral();

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return TypeDescriptors.get().javaLangObject;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_NullLiteral.visit(processor, this);
  }

  private NullLiteral() {}
}
