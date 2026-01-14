/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.j2cl.transpiler.ast.AstUtils.isKotlinUnitInstanceAccess;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveVoid;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.ReturnStatement;

/**
 * Removes return values from {@link MethodLike}s which have a void return.
 *
 * <p>This will rewrite: {@code void foo() { return bar(); } } into: {@code void foo() { { bar();
 * return; } } }
 *
 * <p>The primary use-case for this is for Unit functions coming from Kotlin which can have explicit
 * return values despite ultimately having a `void` return type.
 */
public final class RemoveReturnValuesFromVoidMethods extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteReturnStatement(ReturnStatement returnStatement) {
            if (returnStatement.getExpression() == null) {
              return returnStatement;
            }
            var enclosingMethod =
                (MethodLike) checkNotNull(getParent(MethodLike.class::isInstance));
            if (isPrimitiveVoid(enclosingMethod.getDescriptor().getReturnTypeDescriptor())) {

              // We can elide references to Unit.INSTANCE being returned if it's a void return.
              // This is extremely common in Kotlin code so it's best to not leave it to have to be
              // cleaned up later.
              if (isKotlinUnitInstanceAccess(returnStatement.getExpression())) {
                return ReturnStatement.newBuilder()
                    .setSourcePosition(returnStatement.getSourcePosition())
                    .build();
              }

              return Block.newBuilder()
                  .addStatement(
                      returnStatement
                          .getExpression()
                          .makeStatement(returnStatement.getSourcePosition()))
                  .addStatement(
                      ReturnStatement.newBuilder()
                          .setSourcePosition(returnStatement.getSourcePosition())
                          .build())
                  .build();
            }
            return returnStatement;
          }
        });
  }
}
