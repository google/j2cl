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
package com.google.j2cl.generator;

import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.TypeReference;

/**
 * Utility functions to transpile the j2cl AST.
 */
public class TranspilerUtils {
  public static String getSourceName(TypeReference typeReference) {
    // TODO(rluble): Stub implementation. Needs to be implemented for the cases in which a
    // class might be refered by multiple different type references.
    // TODO(rluble): See if the canonical name concept can be avoided in our AST but converting
    // to canonical type references at AST construction.
    return typeReference.getSourceName();
  }

  /**
   * Returns the unqualified name that will be used in JavaScript.
   */
  public static String getClassName(TypeReference typeReference) {
    //TODO(rluble): Stub implementation.
    return typeReference.getSimpleName();
  }

  /**
   * Returns the JsDoc type name.
   */
  public static String getJsDocName(TypeReference typeReference) {
    //TODO: to be implemented.
    return getClassName(typeReference);
  }

  /**
   * Returns the mangled name.
   */
  public static String getMangledName(TypeReference typeReference) {
    //TODO(rluble): Stub implementation.
    return typeReference.getSourceName().replace('.', '_');
  }

  /**
   * Returns the relative output path for a given compilation unit.
   */
  public static String getOutputPath(CompilationUnit compilationUnit) {
    String unitName = compilationUnit.getName();
    String packageName = compilationUnit.getPackageName();
    return packageName.replace('.', '/') + "/" + unitName;
  }

  private TranspilerUtils() {}
}
