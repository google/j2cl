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

/** Boolean literal node. */
@Visitable
public class BooleanLiteral extends Literal {
  private static final ThreadLocal<BooleanLiteral> FALSE =
      ThreadLocal.withInitial(() -> new BooleanLiteral(false));
  private static final ThreadLocal<BooleanLiteral> TRUE =
      ThreadLocal.withInitial(() -> new BooleanLiteral(true));
  private final boolean value;

  private BooleanLiteral(boolean value) {
    this.value = value;
  }

  public static BooleanLiteral get(boolean value) {
    return value ? TRUE.get() : FALSE.get();
  }

  public boolean getValue() {
    return value;
  }

  @Override
  public String getSourceText() {
    return value ? "true" : "false";
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return PrimitiveTypes.BOOLEAN;
  }

  @Override
  public Expression prefixNot() {
    return get(!value);
  }

  @Override
  public BooleanLiteral clone() {
    // Boolean literals are value types do not need to be actually cloned.
    return this;
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_BooleanLiteral.visit(processor, this);
  }
}
