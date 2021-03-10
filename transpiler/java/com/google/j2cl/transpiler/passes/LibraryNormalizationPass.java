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

import com.google.j2cl.transpiler.ast.Library;

/** The base class for passes that operate on the whole library at once. */
public abstract class LibraryNormalizationPass extends NormalizationPass {
  public final void execute(Library library) {
    applyTo(library);
  }

  public void applyTo(Library library) {
    library.getCompilationUnits().forEach(this::execute);
  }
}
