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
package com.google.j2cl.transpiler.ast;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Ascii;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import java.util.HashMap;
import java.util.Map;

/** Implements lazy initialization of String literals. */
public class StringLiteralGettersCreator {

  private final Map<String, MethodDescriptor> literalMethodByString = new HashMap<>();

  public Map<String, MethodDescriptor> getLiteralMethodByString() {
    return literalMethodByString;
  }

  /**
   * Returns the descriptor for the getter of {@code stringLiteral}, creating it if it did not
   * exist.
   */
  @CanIgnoreReturnValue
  public MethodDescriptor getOrCreateLiteralMethod(
      Type type, StringLiteral stringLiteral, boolean synthesizeMethod) {
    String value = stringLiteral.getValue();
    if (literalMethodByString.containsKey(value)) {
      return literalMethodByString.get(value);
    }

    String snippet = createSnippet(type, value);
    MethodDescriptor getLiteralMethod =
        getLazyStringLiteralGettterMethodDescriptor(
            type.getTypeDescriptor(), "$getString_" + snippet);

    if (synthesizeMethod) {
      type.synthesizeLazilyInitializedField(
          "$string_" + snippet, createStringFromLiteral(value), getLiteralMethod);
    }

    literalMethodByString.put(value, getLiteralMethod);

    return getLiteralMethod;
  }

  private MethodCall createStringFromLiteral(String value) {
    if (!isValidUtf8String(value)) {
      // Strings represented natively in the wasm as string.const need to be valid UTF8 strings. In
      // the case it is not, emit it by initializing from a char array.
      return RuntimeMethods.createStringFromWasmArrayMethodCall(value.toCharArray());
    }
    return RuntimeMethods.createStringFromJsStringMethodCall(new StringLiteral(value));
  }

  private static boolean isValidUtf8String(String value) {
    // Malformed UTF8 strings are sanitized in the .getBytes() api and if the string was malformed
    // it won't convert back to the same string.
    return new String(value.getBytes(UTF_8), UTF_8).equals(value);
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
}
