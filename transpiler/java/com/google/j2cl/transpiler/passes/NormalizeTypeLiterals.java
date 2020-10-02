/*
 * Copyright 2018 Google Inc.
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

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.RuntimeMethods;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.TypeLiteral;

/** Replaces type literal expression with the corresponding method call to the runtime. */
public class NormalizeTypeLiterals extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
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
}
