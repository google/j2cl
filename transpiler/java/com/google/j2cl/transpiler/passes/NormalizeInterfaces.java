/*
 * Copyright 2021 Google Inc.
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

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Type;

/** Removes methods that override java.lang.Object methods from interfaces. */
public class NormalizeInterfaces extends NormalizationPass {

  // TODO(b/322906767): Remove when the bug is fixed.
  private static final boolean PRESERVE_EQUALS_FOR_JSTYPE_INTERFACE =
      "true"
          .equals(
              System.getProperty(
                  "com.google.j2cl.transpiler.backend.kotlin.preserveEqualsForJsTypeInterface"));

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitType(Type type) {
            // Not touching JsTypes to keep the JS contract intact.
            if (type.isInterface()
                && (!PRESERVE_EQUALS_FOR_JSTYPE_INTERFACE || !type.getDeclaration().isJsType())) {
              type.getMembers()
                  .removeIf(member -> member.getDescriptor().isOrOverridesJavaLangObjectMethod());
            }
          }
        });
  }
}
