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
package com.google.j2cl.generator;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodReference;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility functions to transpile the j2cl AST.
 */
public class TranspilerUtils {
  public static String getSourceName(TypeReference typeRef) {
    // TODO(rluble): Stub implementation. Needs to be implemented for the cases in which a
    // class might be refered by multiple different type references.
    // TODO(rluble): See if the canonical name concept can be avoided in our AST but converting
    // to canonical type references at AST construction.
    return typeRef.getSourceName();
  }

  /**
   * Returns the unqualified name that will be used in JavaScript.
   */
  public static String getClassName(TypeReference typeRef) {
    //TODO(rluble): Stub implementation.
    return typeRef.getSimpleName();
  }

  /**
   * Returns the JsDoc type name.
   */
  public static String getJsDocName(TypeReference typeRef) {
    // TODO: Incomplete implementation.
    if (typeRef.isArray()) {
      return String.format(
          "%s%s%s",
          Strings.repeat("Array<", typeRef.getDimensions()),
          getJsDocName(typeRef.getLeafTypeRef()),
          Strings.repeat(">", typeRef.getDimensions()));
    }
    switch (typeRef.getSourceName()) {
      case "int":
      case "double":
      case "float":
      case "short":
        return "number";
      case "java.lang.String":
        return "string";
    }
    return getClassName(typeRef);
  }

  /**
   * Returns the relative output path for a given compilation unit.
   */
  public static String getOutputPath(CompilationUnit compilationUnit) {
    String unitName = compilationUnit.getName();
    String packageName = compilationUnit.getPackageName();
    return packageName.replace('.', '/') + "/" + unitName;
  }

  private static final Import IMPORT_CLASS = new Import("Class", "gen.java.lang.ClassModule");
  private static final Import IMPORT_BOOTSTRAP_UTIL =
      new Import("Util", "nativebootstrap.UtilModule");

  /**
   * Returns the relative output path for a given compilation unit.
   */
  public static Set<Import> getImports(CompilationUnit compilationUnit) {
    Set<Import> imports = new TreeSet<>();
    imports.add(IMPORT_CLASS);
    imports.add(IMPORT_BOOTSTRAP_UTIL);
    imports.addAll(ImportGatheringVisitor.gatherImports(compilationUnit));
    return imports;
  }

  public static String getParameterList(Method method) {
    List<String> parameterNameList =
        Lists.transform(
            method.getParameters(),
            new Function<Variable, String>() {
              @Override
              public String apply(Variable variable) {
                return variable.getName();
              }
            });
    return Joiner.on(", ").join(parameterNameList);
  }

  /**
   * Returns whether the $clinit function should be rewritten as NOP.
   */
  public static boolean needRewriteClinit(JavaType type) {
    for (Field staticField : type.getStaticFields()) {
      if (staticField.hasInitializer()) {
        return true;
      }
    }

    // TODO: also check the static blocks here.

    return false;
  }

  /**
   * Returns whether the specified constructor has a this() call.
   */
  public static boolean hasThisCall(Method method) {
    if (method.getBody().getStatements().isEmpty()) {
      return false;
    }
    Statement firstStatement = method.getBody().getStatements().get(0);
    if (!(firstStatement instanceof ExpressionStatement)) {
      return false;
    }
    Expression expression = ((ExpressionStatement) firstStatement).getExpression();
    if (!(expression instanceof MethodCall)) {
      return false;
    }
    MethodReference methodRef = ((MethodCall) expression).getTarget();
    return methodRef.isConstructor()
        && methodRef
            .getEnclosingClassRef()
            .equals(method.getSelfReference().getEnclosingClassRef());
  }

  /**
   * Return the String with first letter capitalized.
   */
  public static String toProperCase(String string) {
    if (string.isEmpty()) {
      return string;
    } else if (string.length() == 1) {
      return string.toUpperCase();
    }
    return string.substring(0, 1).toUpperCase() + string.substring(1, string.length());
  }

  private TranspilerUtils() {}
}
