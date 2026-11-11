/*
 * Copyright 2026 Google Inc.
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
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;

/**
 * Rejects calls to methods annotated as {@code @NotEmulated}.
 *
 * <p>Such methods only exist to satisfy the compilers and would fail at runtime.
 */
public final class NotEmulatedRestrictionsChecker {
  private static final String NOT_EMULATED_ANNOTATION_NAME =
      "javaemul.internal.annotations.NotEmulated";

  private NotEmulatedRestrictionsChecker() {}

  public static void check(Library library, Problems problems) {
    library.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethodCall(MethodCall methodCall) {
            MethodDescriptor target = methodCall.getTarget().getDeclarationDescriptor();
            if (!target.hasAnnotation(NOT_EMULATED_ANNOTATION_NAME)) {
              return;
            }

            problems.error(
                methodCall.getSourcePosition(),
                "'%s' is not emulated.",
                target.getReadableDescription());
          }
        });
  }
}
