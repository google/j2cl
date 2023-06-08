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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Class for new instance expression.
 */
@Visitable
public class NewInstance extends Invocation {
  @Visitable @Nullable Type anonymousInnerClass = null;

  private NewInstance(
      Expression qualifier,
      MethodDescriptor constructorMethodDescriptor,
      List<Expression> arguments,
      Type anonymousInnerClass) {
    super(qualifier, constructorMethodDescriptor, arguments);
    this.anonymousInnerClass = anonymousInnerClass;
  }

  @Override
  public DeclaredTypeDescriptor getTypeDescriptor() {
    return (DeclaredTypeDescriptor) getTarget().getReturnTypeDescriptor();
  }

  public Type getAnonymousInnerClass() {
    return anonymousInnerClass;
  }

  @Override
  public NewInstance clone() {
    // clone() can only be called after anonymous inner types are normalized away. Even if the
    // anonymous class was duplicated and the name changed, that would imply a semantic difference
    // in Java (.getClass() would return different values).
    // In general we don't support unrestricted cloning (there are other nodes that have
    // restrictions too) so for the time being this node can only be cloned when it is safe to do
    // so.
    checkState(anonymousInnerClass == null);
    return new NewInstance(
        AstUtils.clone(qualifier), getTarget(), AstUtils.clone(arguments), anonymousInnerClass);
  }

  @Override
  Builder createBuilder() {
    return new Builder(this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_NewInstance.visit(processor, this);
  }

  /**
   * A Builder for NewInstance.
   *
   * <p>Takes care of the busy work of keeping argument list and method descriptor parameter types
   * list in sync.
   */
  public static class Builder extends Invocation.Builder<Builder, NewInstance> {
    private Type anonymousInnerClass;

    public static Builder from(NewInstance newInstance) {
      return new Builder(newInstance);
    }

    public static Builder from(MethodDescriptor constructorDescriptor) {
      Builder builder = new Builder();
      builder.setTarget(constructorDescriptor);
      return builder;
    }

    public Builder setAnonymousInnerClass(Type anonymousInnerClass) {
      this.anonymousInnerClass = anonymousInnerClass;
      return this;
    }

    @Override
    public NewInstance build() {
      return new NewInstance(getQualifier(), getTarget(), getArguments(), anonymousInnerClass);
    }

    private Builder(NewInstance newInstance) {
      super(newInstance);
      this.anonymousInnerClass = newInstance.anonymousInnerClass;
    }

    private Builder() {}
  }
}
