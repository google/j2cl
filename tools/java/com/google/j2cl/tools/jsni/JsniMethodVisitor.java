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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptors;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.frontend.JdtUtils;
import com.google.j2cl.generator.ManglingNameUtils;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Visitor that visit native methods and extract all information we need of the jsni blocks.
 */
public class JsniMethodVisitor extends ASTVisitor {
  // Pattern used to handle jsni references.
  // For the time being, we just handle simple case. We don't handle the cases where the pattern
  // can match something in a comment or between quotes.
  private static Pattern jsniRefPattern = Pattern.compile("@[\\p{Alnum}_$.]*(?:\\[\\])*::");

  private final String javaCode;
  private final Multimap<String, JsniMethod> jsniMethodsByType;

  public JsniMethodVisitor(String javaCode) {
    this.javaCode = javaCode;
    jsniMethodsByType = ArrayListMultimap.create();
  }

  @Override
  public void endVisit(MethodDeclaration method) {
    if (Modifier.isNative(method.getModifiers())) {
      MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(method.resolveBinding());
      List<String> parameterNames = extractParameterNames(method);
      if (TypeDescriptors.isBoxedTypeAsJsPrimitives(
          methodDescriptor.getEnclosingClassTypeDescriptor()) && !methodDescriptor.isStatic()) {
        parameterNames.add(0, "$thisArg");
        methodDescriptor = MethodDescriptors.makeStaticMethodDescriptor(methodDescriptor);
      }
      String methodMangledName = ManglingNameUtils.getMangledName(methodDescriptor);
      String javascriptBlock = extractJavascriptBlock(method);

      if (javascriptBlock == null) {
        // Looks like this is a native JsMethod.
        return;
      }

      // no jsni references ?
      checkJsConvertibility(javascriptBlock, method);

      pushJnsiMethod(
          computeTypeFqn(method),
          new JsniMethod(
              methodMangledName,
              parameterNames,
              javascriptBlock,
              methodDescriptor.isStatic()));
    }
  }

  // TODO (dramaix) check and maybe convert $wnd and $doc references ?
  private void checkJsConvertibility(String javascriptBlock, MethodDeclaration method) {
    Matcher jsniRefMatcher = jsniRefPattern.matcher(javascriptBlock);
    if (jsniRefMatcher.find()) {
      throw new RuntimeException(
          String.format(
              "We cannot convert jsni method containing jsni references. Jsni reference [%s...] "
                  + "found in method [%s] in class [%s]",
              jsniRefMatcher.group(),
              method,
              computeTypeFqn(method)));
    }
  }

  private String computeTypeFqn(MethodDeclaration method) {
    return method.resolveBinding().getDeclaringClass().getBinaryName();
  }

  private void pushJnsiMethod(String typeName, JsniMethod jsniMethod) {
    jsniMethodsByType.put(typeName, jsniMethod);
  }

  @SuppressWarnings("unchecked")
  private List<String> extractParameterNames(MethodDeclaration method) {
    List<String> parameters = new ArrayList<>();
    for (SingleVariableDeclaration p : (List<SingleVariableDeclaration>) method.parameters()) {
      parameters.add(p.getName().getIdentifier());
    }

    return parameters;
  }

  private String extractJavascriptBlock(MethodDeclaration method) {
    int start = method.getStartPosition();
    int end = start + method.getLength();

    String methodCode = javaCode.substring(start, end).trim();

    int startPos = methodCode.indexOf("/*-{");
    int endPos = methodCode.lastIndexOf("}-*/");
    if (startPos < 0 || endPos < 0) {
      return null;
    }

    startPos += 4; // move up after the open brace

    return methodCode.substring(startPos, endPos);
  }

  public Multimap<String, JsniMethod> getJsniMethodsByType() {
    return jsniMethodsByType;
  }
}
