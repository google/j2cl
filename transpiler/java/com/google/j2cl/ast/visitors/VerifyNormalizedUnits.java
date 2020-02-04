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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Predicates;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.Type;

/** Verifies that the AST satisfies the normalization invariants. */
public class VerifyNormalizedUnits {

  public static void applyTo(CompilationUnit compilationUnit) {
    // All native methods should be empty.
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethod(Method method) {
            checkState(!method.isNative() || method.getBody().isEmpty());
          }
        });

    for (Type type : compilationUnit.getTypes()) {
      // Native and JsFunction types should have been removed from the AST.
      checkState(!type.isNative());
      checkState(!type.isJsFunctionInterface());
      // JsEnum only contains the enum fields.
      if (type.isJsEnum()) {
        checkState(
            type.getMembers().stream().allMatch(Predicates.and(Member::isField, Member::isStatic)));
      }
    }
  }

  private VerifyNormalizedUnits() {}
}
