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
import com.google.common.io.Files;
import com.google.j2cl.frontend.JdtUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * Drives traversal of a CompilationUnit and Java file to extract native methods.
 */
public class NativeMethodExtractor {

  public static Multimap<String, JsniMethod> getJsniMethodsByType(
      String fileName,
      CompilationUnit compilationUnit,
      Iterable<ITypeBinding> wellKnownTypeBindings) {
    JdtUtils.initTypeDescriptors(compilationUnit.getAST(), wellKnownTypeBindings);
    JsniMethodVisitor methodVisitor = new JsniMethodVisitor(readJavaCode(fileName));
    compilationUnit.accept(methodVisitor);

    return methodVisitor.getJsniMethodsByType();
  }

  private static String readJavaCode(String fileName) {
    try {
      return Files.asCharSource(new File(fileName), Charset.forName("UTF-8")).read();
    } catch (IOException ex) {
      throw new RuntimeException("impossible to read the file " + fileName, ex);
    }
  }
}
