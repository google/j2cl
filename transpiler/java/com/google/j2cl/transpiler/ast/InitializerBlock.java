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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.List;

/** A node that represents an initializer block. */
@Visitable
public class InitializerBlock extends Member implements MethodLike {
  @Visitable Block body;
  @Visitable MethodDescriptor methodDescriptor;

  private InitializerBlock(
      SourcePosition sourcePosition, Block body, MethodDescriptor methodDescriptor) {
    super(sourcePosition);
    this.body = checkNotNull(body);
    this.methodDescriptor = checkNotNull(methodDescriptor);
  }

  @Override
  public Block getBody() {
    return body;
  }

  @Override
  public boolean isInitializerBlock() {
    return true;
  }

  @Override
  public MethodDescriptor getDescriptor() {
    return methodDescriptor;
  }

  @Override
  public List<Variable> getParameters() {
    return ImmutableList.of();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_InitializerBlock.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for InitializerBlock. */
  public static class Builder {
    private Block body;
    private SourcePosition sourcePosition;
    private MethodDescriptor methodDescriptor;

    public static Builder from(InitializerBlock initializerBlock) {
      return newBuilder()
          .setBody(initializerBlock.getBody())
          .setSourcePosition(initializerBlock.getSourcePosition())
          .setDescriptor(initializerBlock.getDescriptor());
    }

    public Builder setDescriptor(MethodDescriptor methodDescriptor) {
      this.methodDescriptor = methodDescriptor;
      return this;
    }

    public Builder setBody(Block body) {
      this.body = body;
      return this;
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }


    public InitializerBlock build() {
      checkState(body != null);
      checkState(sourcePosition != null);
      return new InitializerBlock(sourcePosition, body, methodDescriptor);
    }
  }
}
