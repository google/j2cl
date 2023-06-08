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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Refers a constructor in Javascript. */
@Visitable
public class JavaScriptConstructorReference extends Expression {
  private final TypeDeclaration typeDeclaration;

  public JavaScriptConstructorReference(TypeDeclaration typeDeclaration) {
    this.typeDeclaration = checkNotNull(typeDeclaration);
  }

  @Override
  public DeclaredTypeDescriptor getTypeDescriptor() {
    return TypeDescriptors.get().nativeFunction.toNonNullable();
  }

  @Override
  public boolean isIdempotent() {
    return true;
  }

  @Override
  public boolean isEffectivelyInvariant() {
    return true;
  }

  @Override
  public Precedence getPrecedence() {
    // A JavaScript constructor might be a qualified expression, so it is treated as a member
    // access.
    return Precedence.MEMBER_ACCESS;
  }

  @Override
  public JavaScriptConstructorReference clone() {
    return new JavaScriptConstructorReference(typeDeclaration);
  }

  public TypeDeclaration getReferencedTypeDeclaration() {
    return typeDeclaration;
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_JavaScriptConstructorReference.visit(processor, this);
  }
}
