/*
 * Copyright 2022 Google Inc.
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

import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;

/** Checks and throws errors for constructs which can not be transpiled to Kotlin. */
public final class J2ktRestrictionsChecker {
  private J2ktRestrictionsChecker() {}

  public static void check(Library library, Problems problems) {
    library.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterMethod(Method method) {
            checkMethod(method, problems);
            return true;
          }
        });
  }

  private static void checkMethod(Method method, Problems problems) {
    if (method.isConstructor()
        && !method.getDescriptor().getTypeParameterTypeDescriptors().isEmpty()) {
      problems.error(
          method.getSourcePosition(),
          "Constructor '%s' cannot declare type variables.",
          method.getReadableDescription());
    }
  }
}
