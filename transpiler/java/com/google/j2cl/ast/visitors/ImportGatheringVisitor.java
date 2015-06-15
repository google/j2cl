/*
 * Copyright 2015 Google Inc.
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

import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Import;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.TypeReference;

/**
 * Traverses a CompilationUnit and gathers imports for all its referenced types.
 */
// TODO: turn into an actual Visitor once we have such a framework.
public class ImportGatheringVisitor {
  public static void run(CompilationUnit compilationUnit) {
    compilationUnit.addImport(Import.IMPORT_CLASS);
    compilationUnit.addImport(Import.IMPORT_BOOTSTRAP_UTIL);

    for (JavaType type : compilationUnit.getTypes()) {
      TypeReference superTypeReference = type.getSuperType();
      if (superTypeReference != null) {
        compilationUnit.addImport(new Import(superTypeReference));
      }
      for (TypeReference superInterfaceReference : type.getSuperInterfaces()) {
        compilationUnit.addImport(new Import(superInterfaceReference));
      }
    }
  }
}
