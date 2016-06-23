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

/**
 * Class for field access.
 */
@Visitable
public class FieldAccess extends Expression implements MemberReference {
  @Visitable Expression qualifier;
  @Visitable FieldDescriptor targetFieldDescriptor;

  public FieldAccess(Expression qualifier, FieldDescriptor targetFieldDescriptor) {
    this.targetFieldDescriptor = checkNotNull(targetFieldDescriptor);
    this.qualifier = AstUtils.getExplicitQualifier(qualifier, targetFieldDescriptor);
  }

  @Override
  public Expression getQualifier() {
    return qualifier;
  }

  /**
   * Would normally be named getTargetFieldDescriptor() but in this situation it was more important
   * to implement the MemberReference interface.
   */
  @Override
  public FieldDescriptor getTarget() {
    return targetFieldDescriptor;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return targetFieldDescriptor.getTypeDescriptor();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_FieldAccess.visit(processor, this);
  }
}
