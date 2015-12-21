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

import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;

import java.util.List;

/**
 * This class is injected into the template to allow source generation on ast nodes.
 */
public class SourceGenerator {
  private GenerationEnvironment environment;

  public SourceGenerator(GenerationEnvironment envioronment) {
    this.environment = envioronment;
  }

  public String toSource(Statement statement) {
    SourceBuilder tempBuilder = new SourceBuilder();
    StatementTransformer.transform(statement, environment, tempBuilder);
    return tempBuilder.build();
  }

  public String toSource(Expression expression) {
    return ExpressionTransformer.transform(expression, environment);
  }

  public String toSource(Variable variable) {
    return environment.aliasForVariable(variable);
  }

  public String getJsDocName(TypeDescriptor typeDescriptor) {
    return JsDocNameUtils.getJsDocName(typeDescriptor, environment);
  }

  public String getJsDocName(TypeDescriptor typeDescriptor, boolean shouldUseClassName) {
    return JsDocNameUtils.getJsDocName(typeDescriptor, shouldUseClassName, environment);
  }

  public String getJsDocNames(List<TypeDescriptor> typeDescriptors) {
    return JsDocNameUtils.getJsDocNames(typeDescriptors, environment);
  }
}
