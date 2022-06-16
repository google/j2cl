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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.LabelReference;
import com.google.j2cl.transpiler.ast.NameDeclaration;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Reference;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.VariableReference;

/** Verifies that variables and labels are referenced within their scopes. */
public class VerifyReferenceScoping extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ScopeAwareVisitor<NameDeclaration>() {

          @Override
          public boolean enterLabelReference(LabelReference labelReference) {
            checkReference(labelReference);
            return false;
          }

          @Override
          public boolean enterVariableReference(VariableReference variableReference) {
            checkReference(variableReference);
            return false;
          }

          private void checkReference(Reference<? extends NameDeclaration> reference) {
            // Verify that the reference references a declaration that is in scope.
            if (isElementInScope(reference.getTarget())) {
              return;
            }

            Statement currentStatement = (Statement) getParent(Statement.class::isInstance);
            final Node context =
                currentStatement != null
                    ? currentStatement
                    : getCurrentMember() != null ? getCurrentMember() : getCurrentType();
            throw new AssertionError(
                String.format(
                    "%s %s in %s not defined in enclosing scope.",
                    reference.getTarget().getClass().getSimpleName(),
                    reference.getTarget().getName(),
                    context));
          }

          @Override
          public boolean enterNameDeclaration(NameDeclaration declaration) {
            // Check that the name is declared only once, and was not accidentally duplicated.
            checkState(
                addElementsToScope(declaration),
                "%s %s already in scope.",
                declaration.getClass().getSimpleName(),
                declaration.getName());
            return true;
          }
        });
  }
}
