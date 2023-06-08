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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Context;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Abstract base class for class members. */
@Visitable
@Context
public abstract class Member extends Node
    implements HasJsNameInfo, HasSourcePosition, HasReadableDescription {
  private final SourcePosition sourcePosition;

  public Member(SourcePosition sourcePosition) {
    this.sourcePosition = checkNotNull(sourcePosition);
  }

  public abstract MemberDescriptor getDescriptor();

  public boolean isAbstract() {
    return false;
  }

  public boolean isConstructor() {
    return false;
  }

  public boolean isField() {
    return false;
  }

  public boolean isEnumField() {
    return false;
  }

  public boolean isMethod() {
    return false;
  }

  public boolean isInitializerBlock() {
    return false;
  }

  @Override
  public final boolean isNative() {
    return getDescriptor().isNative();
  }

  public final boolean isStatic() {
    return getDescriptor().isStatic();
  }

  public final String getQualifiedBinaryName() {
    return getDescriptor().getQualifiedBinaryName();
  }

  public final String getMangledName() {
    return getDescriptor().getMangledName();
  }

  @Override
  public final String getSimpleJsName() {
    return getDescriptor().getSimpleJsName();
  }

  @Override
  public final String getJsNamespace() {
    return getDescriptor().getJsNamespace();
  }

  @Override
  public String getReadableDescription() {
    return getDescriptor().getReadableDescription();
  }

  @Override
  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_Member.visit(processor, this);
  }
}
