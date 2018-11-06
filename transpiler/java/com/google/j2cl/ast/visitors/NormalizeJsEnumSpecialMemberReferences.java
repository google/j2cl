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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;

/** Normalizes references to JsEnum special members like {@code ordinal()}, {@code value}, etc. */
public class NormalizeJsEnumSpecialMemberReferences extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    normalizeJsEnumSpecialMemberReferences(compilationUnit);
  }

  /** Rewrites references to the special JsEnum members. */
  private void normalizeJsEnumSpecialMemberReferences(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            if (AstUtils.isJsEnumCustomValueField(fieldAccess.getTarget())) {
              // Expression q.value gets rewritten to just q.
              return fieldAccess.getQualifier();
            }

            return fieldAccess;
          }

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            if (!methodCall.getQualifier().getTypeDescriptor().isJsEnum()) {
              return methodCall;
            }

            // Get the declaration descriptor for the target.
            MethodDescriptor targetDescriptor = methodCall.getTarget().getDeclarationDescriptor();
            String methodSignature = targetDescriptor.getMethodSignature();

            // The methods Enum.ordinal() and Enum.compareTo(Enum) are only allowed on
            // non native JsEnums that do not have custom value.
            if (methodSignature.equals("ordinal()")) {
              // Expression q.ordinal() gets rewritten to just q.
              return methodCall.getQualifier();
            }

            return methodCall;
          }
        });
  }
}
