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
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.HasName;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import com.google.j2cl.transpiler.backend.common.UniqueNamesResolver;
import java.util.List;
import java.util.Map;

/**
 * A base class for JavaScript source generators. We may have two subclasses, which are
 * for Header and Impl generation.
 */
public abstract class JavaScriptGenerator {
  protected final Type type;
  protected final ClosureGenerationEnvironment environment;
  protected final List<Import> imports;
  protected final SourceBuilder sourceBuilder = new SourceBuilder();
  protected final Problems problems;

  public JavaScriptGenerator(Problems problems, Type type, List<Import> imports) {
    this.problems = problems;
    this.type = type;
    this.imports = imports;
    ImmutableSet<String> namesUsedInAliases =
        imports.stream()
            // Take the first component of the alias. Most aliases are not qualified names, but
            // externs might be qualified names and only the first component needs to be considered
            // to avoid top level name clashes.
            .map(
                anImport ->
                    Iterables.get(Splitter.onPattern("\\\\.").split(anImport.getAlias()), 0))
            .collect(toImmutableSet());
    Map<HasName, String> uniqueNameByVariable =
        UniqueNamesResolver.computeUniqueNames(
            Sets.union(namesUsedInAliases, JsKeywords.getKeywords()), type);
    environment = new ClosureGenerationEnvironment(imports, uniqueNameByVariable);
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
