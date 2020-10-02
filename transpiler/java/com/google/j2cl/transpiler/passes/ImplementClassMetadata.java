/*
 * Copyright 2020 Google Inc.
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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;

/** Adds support for getClass(). */
public class ImplementClassMetadata extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    synthesizeClassMetadata(type);
  }

  private static void synthesizeClassMetadata(Type type) {
    if (type.isJsEnum()) {
      // JsEnums don't support class metadata. Operations like Type.class are forbidden for JsEnums.
      return;
    }

    TypeDeclaration targetTypeDeclaration = type.getUnderlyingTypeDeclaration();
    if (targetTypeDeclaration.isJsFunctionInterface()
        || (targetTypeDeclaration.isInterface() && targetTypeDeclaration.isNative())) {
      // JsFunction and Native interfaces do not need class metadata.
      return;
    }

    String name =
        targetTypeDeclaration.isNative()
            // For native types the qualified JavaScript name is more useful to identify the
            // type, in particular for debugging.
            ? targetTypeDeclaration.getQualifiedJsName()
            : targetTypeDeclaration.getQualifiedBinaryName();

    type.addLoadTimeStatement(
        RuntimeMethods.createUtilMethodCall(
                getSetMetadataMethodName(targetTypeDeclaration),
                new JavaScriptConstructorReference(type.getDeclaration()),
                new StringLiteral(name))
            .makeStatement(SourcePosition.NONE));
  }

  private static String getSetMetadataMethodName(TypeDeclaration type) {
    switch (type.getKind()) {
      case INTERFACE:
        return "$setClassMetadataForInterface";
      case ENUM:
        if (!type.isJsEnum()) {
          return "$setClassMetadataForEnum";
        }
        // fallthrough for JsEnum
      case CLASS:
        return "$setClassMetadata";
    }
    throw new AssertionError();
  }
}
