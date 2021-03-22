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
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.transpiler.ast.TypeLiteral;

/** Adds support for type literals and getClass(). */
public class ImplementClassMetadataViaConstructors extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    normalizeTypeLiterals(type);
    synthesizeClassMetadata(type);
  }

  /** Convert type literals into the corresponding calls to the runtime. */
  public static void normalizeTypeLiterals(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteTypeLiteral(TypeLiteral typeLiteral) {
            return convertTypeLiteral(typeLiteral);
          }
        });
  }

  private static Expression convertTypeLiteral(TypeLiteral typeLiteral) {
    TypeDescriptor literalTypeDescriptor = typeLiteral.getReferencedTypeDescriptor();

    if (literalTypeDescriptor.isArray()) {
      return convertArrayTypeLiteral((ArrayTypeDescriptor) literalTypeDescriptor);
    }

    if (literalTypeDescriptor.isNative()) {
      // class literals of native JsTypes.
      BootstrapType proxyType =
          literalTypeDescriptor.isInterface()
              ? BootstrapType.JAVA_SCRIPT_INTERFACE
              : BootstrapType.JAVA_SCRIPT_OBJECT;
      return convertTypeLiteral(proxyType.getDescriptor());
    }

    if (literalTypeDescriptor.isJsFunctionInterface()
        || literalTypeDescriptor.isJsFunctionImplementation()) {
      // class literal for JsFunction interfaces and implementations.
      return convertTypeLiteral(BootstrapType.JAVA_SCRIPT_FUNCTION.getDescriptor());
    }
    return convertTypeLiteral(literalTypeDescriptor);
  }

  private static Expression convertTypeLiteral(TypeDescriptor literalTypeDescriptor) {
    // Class.$get(constructor)
    return RuntimeMethods.createClassGetMethodCall(
        literalTypeDescriptor.getMetadataConstructorReference());
  }

  private static Expression convertArrayTypeLiteral(ArrayTypeDescriptor literalTypeDescriptor) {
    if (literalTypeDescriptor.isUntypedArray()) {
      // class literal of native js type array returns Object[].class
      literalTypeDescriptor = TypeDescriptors.get().javaLangObjectArray;
    }

    // Class.$get(leafConstructor, dimenstions)
    return RuntimeMethods.createClassGetMethodCall(
        literalTypeDescriptor.getLeafTypeDescriptor().getMetadataConstructorReference(),
        NumberLiteral.fromInt(literalTypeDescriptor.getDimensions()));
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
