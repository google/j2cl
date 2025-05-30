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
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Abstracts invocations, i.e. method calls and new instances. */
@Visitable
public abstract class Invocation extends MemberReference {
  @Visitable List<Expression> arguments = new ArrayList<>();
  @Visitable List<TypeDescriptor> typeArguments = new ArrayList<>();

  Invocation(
      Expression qualifier,
      MethodDescriptor target,
      List<Expression> arguments,
      List<TypeDescriptor> typeArguments) {
    super(qualifier, target);
    this.arguments.addAll(arguments);
    this.typeArguments.addAll(typeArguments);
  }

  public final List<Expression> getArguments() {
    return arguments;
  }

  /** Type arguments that are explicitly provided (as opposed to inferred). */
  public List<TypeDescriptor> getTypeArguments() {
    return typeArguments;
  }

  @Override
  public MethodDescriptor getTarget() {
    return (MethodDescriptor) super.getTarget();
  }

  @Override
  public boolean hasSideEffects() {
    return (getQualifier() != null && getQualifier().hasSideEffects())
        || !getTarget().isSideEffectFree();
  }

  @Override
  abstract Builder<?, ?> createBuilder();

  /**
   * Common logic for a builder to create method calls and new instances.
   *
   * <p>Takes care of the busy work of keeping argument list and method descriptor parameter types
   * list in sync.
   */
  public abstract static class Builder<T extends Builder<T, I>, I extends Invocation>
      extends MemberReference.Builder<T, I, MethodDescriptor> {

    private List<Expression> arguments = new ArrayList<>();
    private List<TypeDescriptor> typeArguments = new ArrayList<>();

    public static Builder<?, ?> from(Invocation invocation) {
      return invocation.createBuilder();
    }

    @CanIgnoreReturnValue
    public final T setArguments(Expression... arguments) {
      return setArguments(Arrays.asList(arguments));
    }

    @CanIgnoreReturnValue
    public final T setArguments(List<Expression> arguments) {
      this.arguments.clear();
      this.arguments.addAll(arguments);
      return getThis();
    }

    @CanIgnoreReturnValue
    public final T addArgumentsAndUpdateDescriptor(int index, Expression... argumentExpressions) {
      return addArgumentsAndUpdateDescriptor(index, Arrays.asList(argumentExpressions));
    }

    @CanIgnoreReturnValue
    public final T addArgumentsAndUpdateDescriptor(
        int index, Collection<Expression> argumentExpressions) {
      if (argumentExpressions.isEmpty()) {
        return getThis();
      }

      arguments.addAll(index, argumentExpressions);
      // Add the provided parameters to the proper index position of the existing parameters list.
      return setTarget(
          getTarget()
              .transform(
                  builder ->
                      builder.addParameterTypeDescriptors(
                          index,
                          argumentExpressions.stream()
                              .map(Expression::getTypeDescriptor)
                              .collect(toImmutableList()))));
    }

    @CanIgnoreReturnValue
    public final T addArgumentAndUpdateDescriptor(
        int index, Expression argumentExpression, TypeDescriptor parameterTypeDescriptor) {
      arguments.add(index, argumentExpression);
      // Add the provided parameters to the proper index position of the existing parameters list.

      return setTarget(
          getTarget()
              .transform(
                  builder -> builder.addParameterTypeDescriptors(index, parameterTypeDescriptor)));
    }

    @CanIgnoreReturnValue
    public final T replaceVarargsArgument(Expression... replacementArguments) {
      return replaceVarargsArgument(Arrays.asList(replacementArguments));
    }

    @CanIgnoreReturnValue
    public final T replaceVarargsArgument(List<Expression> replacementArguments) {
      checkState(getTarget().isVarargs());
      int lastArgumentPosition = arguments.size() - 1;
      arguments.remove(lastArgumentPosition);
      arguments.addAll(replacementArguments);
      return getThis();
    }

    protected final List<Expression> getArguments() {
      return arguments;
    }

    @CanIgnoreReturnValue
    public final T setTypeArguments(List<TypeDescriptor> typeArguments) {
      this.typeArguments.clear();
      this.typeArguments.addAll(typeArguments);
      return getThis();
    }

    protected final List<TypeDescriptor> getTypeArguments() {
      return typeArguments;
    }

    Builder(Invocation invocation) {
      super(invocation);
      this.arguments = Lists.newArrayList(invocation.getArguments());
      this.typeArguments = Lists.newArrayList(invocation.getTypeArguments());
    }

    Builder() {}
  }
}
