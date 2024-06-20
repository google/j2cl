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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.visitor.Visitable;
import javax.annotation.Nullable;

/** Abstracts class member reference (i.e. field accesses, method calls and new instances). */
@Visitable
public abstract class MemberReference extends Expression {
  @Visitable @Nullable Expression qualifier;
  @Visitable MemberDescriptor target;

  MemberReference(Expression qualifier, MemberDescriptor target) {
    this.qualifier = qualifier;
    this.target = checkNotNull(target);
  }

  public MemberDescriptor getTarget() {
    return target;
  }

  public final Expression getQualifier() {
    return qualifier;
  }

  @Override
  public Precedence getPrecedence() {
    return Precedence.MEMBER_ACCESS;
  }

  abstract Builder<?, ?, ?> createBuilder();

  /** Common logic for a builder to create method calls, new instances and field accesses. */
  public abstract static class Builder<
      T extends MemberReference.Builder<T, R, D>,
      R extends MemberReference,
      D extends MemberDescriptor> {

    private Expression qualifier;
    private D target;

    public static MemberReference.Builder<?, ?, ?> from(MemberReference memberReference) {
      return memberReference.createBuilder();
    }

    @CanIgnoreReturnValue
    public final T setQualifier(Expression qualifier) {
      this.qualifier = qualifier;
      return getThis();
    }

    @CanIgnoreReturnValue
    public final T setTarget(D target) {
      this.target = target;
      return getThis();
    }

    @CanIgnoreReturnValue
    public T setDefaultInstanceQualifier() {
      if (target.isInstanceMember()) {
        qualifier = new ThisReference(target.getEnclosingTypeDescriptor());
      }
      return getThis();
    }

    public abstract R build();

    @SuppressWarnings("unchecked")
    protected final T getThis() {
      return (T) this;
    }

    protected final Expression getQualifier() {
      return qualifier;
    }

    protected final D getTarget() {
      return target;
    }

    @SuppressWarnings("unchecked")
    Builder(MemberReference memberReference) {
      this.qualifier = memberReference.getQualifier();
      this.target = (D) memberReference.getTarget();
    }

    Builder() {}
  }
}
