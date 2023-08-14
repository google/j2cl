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

package com.google.j2cl.transpiler.frontend.jdt;

import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;

/** Class that encapsulates the results of constructing the AST by JDT. */
public class CompilationUnitsAndTypeBindings {

  private final Map<String, CompilationUnit> compilationUnitByFilePath;
  private final List<ITypeBinding> typeBindings;

  public CompilationUnitsAndTypeBindings(
      Map<String, CompilationUnit> compilationUnitByFilePath, List<ITypeBinding> typeBindings) {
    this.compilationUnitByFilePath = compilationUnitByFilePath;
    this.typeBindings = typeBindings;
  }

  public Map<String, CompilationUnit> getCompilationUnitsByFilePath() {
    return compilationUnitByFilePath;
  }

  public List<ITypeBinding> getTypeBindings() {
    return typeBindings;
  }
}
