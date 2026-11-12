/*
 * Copyright 2018 Google Inc.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.HasSourcePosition;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

/** Interface that describes elements that look like method definitions. */
public interface MethodLike extends HasSourcePosition, HasReadableDescription {
  /** Returns the method descriptor related to the node. */
  MethodDescriptor getDescriptor();

  /** Returns list of parameter declarations. */
  List<Variable> getParameters();

  /** Returns the declaration of the Varargs parameter if any */
  @Nullable
  default Variable getVarargsParameter() {
    if (getDescriptor().isVarargs()) {
      return Iterables.getLast(getParameters());
    }
    return null;
  }

  /** Returns the declaration of the JsVarargs parameter if any */
  @Nullable
  default Variable getJsVarargsParameter() {
    if (getDescriptor().isJsMethodVarargs()) {
      return getVarargsParameter();
    }
    return null;
  }

  /** Returns the body related to the node. */
  Block getBody();

  // Expose `Node.accept` to make it easier to traverse the tree from a MethodLike.
  void accept(Processor processor);

  /** Common logic for builders of method-like constructs. */
  abstract class Builder<B extends Builder<B, M>, M extends MethodLike> {
    protected final List<Variable> parameters = new ArrayList<>();
    private final List<Statement> statements = new ArrayList<>();
    protected SourcePosition sourcePosition;

    protected Builder() {}

    protected Builder(MethodLike methodLike) {
      setParameters(methodLike.getParameters());
      setBody(methodLike.getBody());
      setSourcePosition(methodLike.getSourcePosition());
    }

    @CanIgnoreReturnValue
    public final B setParameters(Variable... parameters) {
      return setParameters(Arrays.asList(parameters));
    }

    @CanIgnoreReturnValue
    public final B setParameters(Collection<Variable> parameters) {
      this.parameters.clear();
      this.parameters.addAll(parameters);
      return getThis();
    }

    /**
     * Sets the statements of the body from {@code body}.
     *
     * <p>A {@code null} body (e.g. for abstract or native methods) results in an empty body. Note
     * that the body always takes the source position of the construct.
     */
    @CanIgnoreReturnValue
    public final B setBody(@Nullable Block body) {
      return setStatements(body == null ? ImmutableList.of() : body.getStatements());
    }

    @CanIgnoreReturnValue
    public final B setStatements(Statement... statements) {
      return setStatements(Arrays.asList(statements));
    }

    @CanIgnoreReturnValue
    public final B setStatements(Collection<Statement> statements) {
      this.statements.clear();
      return addStatements(statements);
    }

    @CanIgnoreReturnValue
    public final B addStatement(int index, Statement statement) {
      this.statements.add(index, statement);
      return getThis();
    }

    @CanIgnoreReturnValue
    public final B addStatements(Statement... statements) {
      return addStatements(Arrays.asList(statements));
    }

    @CanIgnoreReturnValue
    public final B addStatements(Collection<Statement> statements) {
      this.statements.addAll(statements);
      return getThis();
    }

    @CanIgnoreReturnValue
    public final B setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return getThis();
    }

    protected final Block buildBody() {
      return Block.builder().setSourcePosition(sourcePosition).setStatements(statements).build();
    }

    @SuppressWarnings("unchecked")
    private B getThis() {
      return (B) this;
    }

    public abstract M build();
  }
}
