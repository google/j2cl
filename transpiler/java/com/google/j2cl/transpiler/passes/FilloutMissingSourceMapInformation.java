/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;

/**
 * Adds method qualified names to source positions for sourcemaps and fills out missing information
 */
public class FilloutMissingSourceMapInformation extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterFunctionExpression(FunctionExpression functionExpression) {
            SourcePosition sourcePosition = checkNotNull(functionExpression.getSourcePosition());

            MemberDescriptor memberDescriptor = getCurrentMember().getDescriptor();

            String qualifiedBinaryName =
                sourcePosition.getName() != null
                    ? sourcePosition.getName()
                    : String.format(
                        "%s.<lambda in %s>",
                        memberDescriptor.getEnclosingTypeDescriptor().getQualifiedBinaryName(),
                        memberDescriptor.getBinaryName());

            tagStatements(functionExpression.getBody(), qualifiedBinaryName);
            return true;
          }

          @Override
          public boolean enterMember(Member member) {
            if (member.isField() || member.getSourcePosition() == SourcePosition.NONE) {
              return true;
            }

            tagStatements(member, member.getQualifiedBinaryName());
            return true;
          }
        });
  }

  private static void tagStatements(Node node, String methodName) {
    node.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterFunctionExpression(FunctionExpression functionExpression) {
            // Do not recurse into FunctionExpressions.
            return false;
          }

          @Override
          public boolean enterStatement(Statement statement) {
            SourcePosition sourcePosition = statement.getSourcePosition();

            // If there is already a name in the AST do not overwrite
            // Some synthesized methods fill out the name earlier
            if (sourcePosition.getName() != null) {
              return true;
            }

            statement.setSourcePosition(
                SourcePosition.Builder.from(sourcePosition).setName(methodName).build());
            return true;
          }
        });
  }
}
