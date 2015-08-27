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

import com.google.common.collect.Multimap;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * This class parses a Java file and extract the information from the native methods included
 * in this file.
 */
public class NativeMethodParser {
  //TODO (dramaix): replace by the right package once the annotation exists.
  public static final String JS_METHOD_PACKAGE = "com.google.j2cl.tools.jsni";
  public static final String JS_METHOD_NAME = "JsMethod";
  public static final String JS_METHOD_FQN = JS_METHOD_PACKAGE + "." + JS_METHOD_NAME;

  public Multimap<String, JsniMethod> parse(File javaFile) {
    try {
      return doParse(readFileToString(javaFile));
    } catch (IOException ex) {
      throw new RuntimeException("impossible to read the file " + javaFile, ex);
    }
  }

  private Multimap<String, JsniMethod> doParse(final String javaCode) {
    ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setSource(javaCode.toCharArray());
    parser.setKind(ASTParser.K_COMPILATION_UNIT);

    CompilationUnit cu = (CompilationUnit) parser.createAST(null);

    JsMethodImportVisitor importVisitor = new JsMethodImportVisitor();
    cu.accept(importVisitor);

    JsniMethodVisitor visitor = new JsniMethodVisitor(javaCode, importVisitor.isJsMethodImported());
    cu.accept(visitor);

    return visitor.getJsniMethodsBytType();
  }

  private String readFileToString(File file) throws IOException {
    CharSource fileSource = Files.asCharSource(file, Charset.forName("UTF-8"));
    return fileSource.read();
  }
}
