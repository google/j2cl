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

import static com.google.j2cl.tools.jsni.NativeMethodParser.JS_METHOD_FQN;
import static com.google.j2cl.tools.jsni.NativeMethodParser.JS_METHOD_NAME;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
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
  private final boolean jsMethodImported;
  private final Deque<String> typeStack;
  private String jsMethodName;
  private String currentTypeName;
  private String currentPackageName;

  public JsniMethodVisitor(String javaCode, boolean jsMethodImported) {
    this.javaCode = javaCode;
    this.jsMethodImported = jsMethodImported;
    jsniMethodsByType = ArrayListMultimap.create();
    typeStack = new ArrayDeque<>();
  }

  @Override
  public boolean visit(PackageDeclaration node) {
    currentPackageName = node.getName().getFullyQualifiedName();
    return super.visit(node);
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    currentTypeName = null;
    typeStack.push(node.getName().getIdentifier());
    return true;
  }

  @Override
  public void endVisit(TypeDeclaration node) {
    typeStack.pop();
    currentTypeName = null;
  }

  @Override
  public boolean visit(MethodDeclaration method) {
    if (Modifier.isNative(method.getModifiers())) {
      jsMethodName = null;
      // visit annotations only on native methods.
      return true;
    }

    return false;
  }

  @Override
  public void endVisit(MethodDeclaration method) {
    if (Modifier.isNative(method.getModifiers())) {
      String methodName = jsMethodName != null ? jsMethodName : method.getName().getIdentifier();
      String javascriptBlock = extractJavascriptBlock(method);

      // no jsni references ?
      checkJsConvertibility(javascriptBlock, method);

      List<String> parameters = extractParameters(method);

      pushJnsiMethod(
          new JsniMethod(
              methodName, parameters, javascriptBlock, Modifier.isStatic(method.getModifiers())));
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
              computeTypeFqn()));
    }
  }

  private void pushJnsiMethod(JsniMethod jsniMethod) {
    String typeName = computeTypeFqn();

    jsniMethodsByType.put(typeName, jsniMethod);
  }

  private String computeTypeFqn() {
    if (currentTypeName == null) {
      StringBuilder nameBuilder = new StringBuilder();
      for (Iterator<String> it = typeStack.descendingIterator(); it.hasNext(); ) {
        String type = it.next();
        if (nameBuilder.length() > 0) {
          nameBuilder.append("$");
        }
        nameBuilder.append(type);
      }

      currentTypeName = nameBuilder.toString();
    }

    return currentPackageName + "." + currentTypeName;
  }

  @SuppressWarnings("unchecked")
  private List<String> extractParameters(MethodDeclaration method) {
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
      throw new RuntimeException(
          String.format(
              "Native methods require a JavaScript implementation enclosed with /*-{ and }-*/ "
                  + "Method [%s] on class [%s]",
              method,
              computeTypeFqn()));
    }

    startPos += 4; // move up after the open brace

    return methodCode.substring(startPos, endPos);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean visit(NormalAnnotation annotation) {
    if (isJsMethodAnnotation(annotation)) {
      for (MemberValuePair member : (List<MemberValuePair>) annotation.values()) {
        if ("value".equals(member.getName().getIdentifier())) {
          jsMethodName = ((StringLiteral) member.getValue()).getLiteralValue();
        }
      }
    }
    return false;
  }

  @Override
  public boolean visit(SingleMemberAnnotation annotation) {
    if (isJsMethodAnnotation(annotation)) {
      Expression methodName = annotation.getValue();
      jsMethodName = ((StringLiteral) methodName).getLiteralValue();
    }
    return false;
  }

  private boolean isJsMethodAnnotation(Annotation annotation) {
    String annotationType = annotation.getTypeName().getFullyQualifiedName();
    if (jsMethodImported && JS_METHOD_NAME.equals(annotationType)) {
      return true;
    }

    return JS_METHOD_FQN.equals(annotationType);
  }

  public Multimap<String, JsniMethod> getJsniMethodsBytType() {
    return jsniMethodsByType;
  }
}
