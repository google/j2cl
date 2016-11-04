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
package com.google.j2cl.ast;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.SourcePosition;

/** A node that represents an initializer block. */
@Visitable
public class InitializerBlock extends Member {
  @Visitable Block block;
  private final boolean isStatic;

  public InitializerBlock(Block block, boolean isStatic) {
    this.block = checkNotNull(block);
    this.isStatic = isStatic;
  }

  public Block getBlock() {
    return block;
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  @Override
  public boolean isInitializerBlock() {
    return true;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_InitializerBlock.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for InitializerBlock. */
  public static class Builder {
    private Block block;
    private boolean isStatic;
    private SourcePosition sourcePosition = SourcePosition.UNKNOWN;

    public static Builder from(InitializerBlock initializerBlock) {
      return newBuilder()
          .setBlock(initializerBlock.getBlock())
          .setIsStatic(initializerBlock.isStatic())
          .setSourcePosition(initializerBlock.getSourcePosition());
    }

    public Builder setBlock(Block block) {
      this.block = block;
      return this;
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Builder setIsStatic(boolean isStatic) {
      this.isStatic = isStatic;
      return this;
    }

    public InitializerBlock build() {
      checkState(block != null);
      checkState(sourcePosition != null);
      InitializerBlock initializerBlock = new InitializerBlock(block, isStatic);
      initializerBlock.setSourcePosition(sourcePosition);
      return initializerBlock;
    }
  }
}
