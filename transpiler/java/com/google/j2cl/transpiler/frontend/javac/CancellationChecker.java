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
package com.google.j2cl.transpiler.frontend.javac;

import com.google.j2cl.common.Problems;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.comp.CompileStates.CompileState;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;

/** Helper to register custom Javac components that support cancellation. */
public final class CancellationChecker {

  public static void register(Context context, Problems problems) {
    context.put(
        JavaCompiler.compilerKey,
        new Context.Factory<JavaCompiler>() {
          @Override
          public JavaCompiler make(Context c) {
            return new JavaCompiler(c) {
              @Override
              protected boolean shouldStop(CompileState phase) {
                // Use the same check point that the compiler uses to abort on errors.
                problems.abortIfCancelled();
                return super.shouldStop(phase);
              }

              @Override
              public Symbol resolveIdent(ModuleSymbol module, String name) {
                problems.abortIfCancelled();
                return super.resolveIdent(module, name);
              }
            };
          }
        });
  }

  private CancellationChecker() {}
}
