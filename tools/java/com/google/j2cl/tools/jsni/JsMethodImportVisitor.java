/*
 * Copyright 2015 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
package com.google.j2cl.tools.jsni;

import static com.google.j2cl.tools.jsni.NativeMethodParser.JS_METHOD_FQN;
import static com.google.j2cl.tools.jsni.NativeMethodParser.JS_METHOD_PACKAGE;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ImportDeclaration;

/**
 * Visitor visiting the imports of a java file in order to detect if the annotation
 * {@link JsniMethod} is imported.
 */
public class JsMethodImportVisitor extends ASTVisitor {
  private boolean jsMethodImported;

  @Override
  public boolean visit(ImportDeclaration node) {
    if (!jsMethodImported && !node.isStatic()) {
      String importedClass = node.getName().getFullyQualifiedName();
      jsMethodImported =
          node.isOnDemand()
              ? JS_METHOD_PACKAGE.equals(importedClass)
              : JS_METHOD_FQN.equals(importedClass);
    }
    return false;
  }

  public boolean isJsMethodImported() {
    return jsMethodImported;
  }
}
