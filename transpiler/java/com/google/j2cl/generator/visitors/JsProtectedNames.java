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
package com.google.j2cl.generator.visitors;

import com.google.common.collect.Sets;
import java.util.Set;

/**
 * All JavaScript keywords.
 */
public class JsProtectedNames {
  private static final Set<String> protectedNames;

  static {
    protectedNames =
        Sets.newHashSet(
            // Main closure namespace, used at generation time.
            "goog",
            // TODO(rluble): move all uses to ast so that the names are collected uniformly.
            // Names of externs that might used at generation time without a presence in the ast.
            // These should be explicitly avoided.
            "Function",
            // Names of common externs that are currently not explicitly used but they might need to
            // be used in native.js files.
            // used
            "boolean",
            "number",
            "string",
            "window",
            "Array",
            "Object",
            "String",
            // Javascript keywords.
            "abstract",
            "arguments",
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
  }

  public static boolean isLegalName(String s) {
    return !protectedNames.contains(s);
  }
}
