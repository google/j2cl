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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.HasName;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import java.util.List;
import java.util.Map;

/**
 * A base class for JavaScript source generators. We may have two subclasses, which are
 * for Header and Impl generation.
 */
public abstract class JavaScriptGenerator {
  protected final Type type;
  protected final GenerationEnvironment environment;
  protected final List<Import> imports;
  protected final SourceBuilder sourceBuilder = new SourceBuilder();
  protected final Problems problems;

  public JavaScriptGenerator(Problems problems, Type type, List<Import> imports) {
    this.problems = problems;
    this.type = type;
    this.imports = imports;
    Map<HasName, String> uniqueNameByVariable =
        UniqueVariableNamesGatherer.computeUniqueVariableNames(imports, type);
    environment = new GenerationEnvironment(imports, uniqueNameByVariable);
  }

  public Map<SourcePosition, SourcePosition> getSourceMappings() {
    return sourceBuilder.getMappings();
  }

  public Map<MemberDescriptor, SourcePosition> getOutputSourceInfoByMember() {
    return sourceBuilder.getOutputSourceInfoByMember();
  }

  abstract String renderOutput();

  abstract String getSuffix();

  /**
   * @param suppressions file level suppresions. Should be only used as a workaround if an urgent
   *     suppression is needed without needing to wait for JsCompiler release.
   */
  void renderSuppressions(String... suppressions) {
    checkArgument(suppressions.length > 0);
    sourceBuilder.appendLines(
        "/** @fileoverview @suppress {" + Joiner.on(", ").join(suppressions) + "} */");
    sourceBuilder.newLine();
  }
}
