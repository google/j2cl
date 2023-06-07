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

import com.google.common.base.Ascii;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.HashMap;
import java.util.Map;

/** Implements lazy initialization of String literals. */
public class ImplementStringCompileTimeConstants extends LibraryNormalizationPass {

  private final Map<String, MethodDescriptor> literalMethodByString = new HashMap<>();

  @Override
  public void applyTo(Library library) {
    rewriteStringLiterals(library);
  }

  /**
   * Creates a method for each unique string literal in the program, and replaces each of them by a
   * call to the created getter.
   */
  private void rewriteStringLiterals(Library library) {
    library.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessMethod(Method method) {
            // Avoid processing the string literals that are moved to the getter.
            return method.getDescriptor().getOrigin()
                != MethodOrigin.SYNTHETIC_STRING_LITERAL_GETTER;
          }

          @Override
          public Expression rewriteStringLiteral(StringLiteral stringLiteral) {
            return MethodCall.Builder.from(
                    getOrCreateLiteralMethod(getCurrentType(), stringLiteral))
                .build();
          }
        });
  }

  /**
   * Returns the descriptor for the getter of {@code stringLiteral}, creating it if it did not
   * exist.
   */
  private MethodDescriptor getOrCreateLiteralMethod(Type type, StringLiteral stringLiteral) {
    String value = stringLiteral.getValue();
    if (literalMethodByString.containsKey(value)) {
      return literalMethodByString.get(value);
    }

    String snippet = createSnippet(type, value);
    MethodDescriptor getLiteralMethod =
        getLazyStringLiteralGettterMethodDescriptor(
            type.getTypeDescriptor(), "$getString_" + snippet);
    type.synthesizeLazilyInitializedField(
        "$string_" + snippet, synthesizeStringCreation(stringLiteral), getLiteralMethod);

    literalMethodByString.put(value, getLiteralMethod);

    return getLiteralMethod;
  }

  /** Returns the descriptor for the getter of the string literal. */
  private static MethodDescriptor getLazyStringLiteralGettterMethodDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor, String name) {
    return MethodDescriptor.newBuilder()
        .setName(name)
        .setReturnTypeDescriptor(TypeDescriptors.get().javaLangString)
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setOrigin(MethodOrigin.SYNTHETIC_STRING_LITERAL_GETTER)
        .setStatic(true)
        .setSynthetic(true)
        .setSideEffectFree(true)
        .build();
  }

  private final Map<Type, Multiset<String>> snippetsByType = new HashMap<>();

  private String createSnippet(Type type, String value) {
    // Take the first few characters of the string and remove invalid identifier characters.
    String prefix = Ascii.truncate(value, 15, "...").replaceAll("[^A-Za-z0-9.]", "_");

    var typeSnippets = snippetsByType.computeIfAbsent(type, t -> HashMultiset.create());
    int occurrences = typeSnippets.count(prefix);
    typeSnippets.add(prefix);
    if (occurrences > 0) {
      return String.format("|%s|_%d", prefix, occurrences);
    } else {
      return String.format("|%s|", prefix);
    }
  }

  /**
   * Converts the StringLiteral into a call to the runtime to initialize create a String from a char
   * array.
   */
  private static Expression synthesizeStringCreation(StringLiteral stringLiteral) {
    ArrayTypeDescriptor charArrayDescriptor =
        ArrayTypeDescriptor.newBuilder().setComponentTypeDescriptor(PrimitiveTypes.CHAR).build();
    MethodDescriptor fromInternalArray =
        TypeDescriptors.get()
            .javaLangString
            .getMethodDescriptor("fromInternalArray", charArrayDescriptor);
    if (fromInternalArray != null) {
      // TODO(b/272381112): Remove after non-stringref experiment.
      // This is the non-stringref j.l.String.
      return MethodCall.Builder.from(fromInternalArray)
          .setArguments(new ArrayLiteral(charArrayDescriptor, stringLiteral.toCharLiterals()))
          .build();
    }
    return RuntimeMethods.createStringFromJsStringMethodCall(stringLiteral);
  }
}
