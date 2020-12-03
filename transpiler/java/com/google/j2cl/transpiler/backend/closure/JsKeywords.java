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
package com.google.j2cl.transpiler.backend.closure;

import com.google.common.collect.ImmutableSet;

/** All JavaScript keywords and Closure predefined types. */
final class JsKeywords {
  private static final ImmutableSet<String> KEYWORDS =
      ImmutableSet.of(
          // Main closure namespace, used at generation time.
          "goog",
          // TODO(b/80241817): Cleanup when all non KEYWORDS that are used appear in the AST.
          // Names of externs that might used at generation time without a presence in the ast.
          // These should be explicitly avoided.
          "Function",
          // Names of common externs that are currently not explicitly used. These are renamed to
          // avoid confusion and they are also commonly used in native.js files.
          "boolean",
          "number",
          "string",
          "undefined",
          "window",
          "Array",
          "Infinity",
          "NaN",
          "Object",
          "String",
          // Javascript keywords.
          "abstract",
          "arguments",
          "async",
          "await",
          "boolean",
          "break",
          "byte",
          "case",
          "catch",
          "char",
          "class",
          "const",
          "continue",
          "debugger",
          "default",
          "delete",
          "do",
          "double",
          "else",
          "enum",
          "eval",
          "export",
          "exports",
          "extends",
          "false",
          "final",
          "finally",
          "float",
          "for",
          "function",
          "goto",
          "if",
          "implements",
          "import",
          "in",
          "instanceof",
          "int",
          "interface",
          "let",
          "long",
          "native",
          "new",
          "null",
          "package",
          "private",
          "protected",
          "public",
          "return",
          "short",
          "static",
          "super",
          "switch",
          "synchronized",
          "this",
          "throw",
          "throws",
          "transient",
          "true",
          "try",
          "typeof",
          "var",
          "void",
          "volatile",
          "while",
          "with",
          "yield");

  static ImmutableSet<String> getKeywords() {
    return KEYWORDS;
  }

  static boolean isKeyword(String s) {
    return !KEYWORDS.contains(s);
  }

  private JsKeywords() {}
}
