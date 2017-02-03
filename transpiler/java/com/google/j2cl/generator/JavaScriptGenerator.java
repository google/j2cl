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

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatherer;
import com.google.j2cl.generator.visitors.ImportGatherer.ImportCategory;
import com.google.j2cl.generator.visitors.VariableAliasesGatheringVisitor;
import com.google.j2cl.problems.Problems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A base class for JavaScript source generators. We may have two subclasses, which are
 * for Header and Impl generation.
 */
public abstract class JavaScriptGenerator {
  protected final Type type;
  protected GenerationEnvironment environment;
  protected Map<ImportCategory, Set<Import>> importsByCategory;
  protected final SourceBuilder sourceBuilder = new SourceBuilder();
  protected final Problems problems;
  protected final boolean declareLegacyNamespace;

  public JavaScriptGenerator(Problems problems, boolean declareLegacyNamespace, Type type) {
    this.problems = problems;
    this.declareLegacyNamespace = declareLegacyNamespace;
    this.type = type;
    importsByCategory = ImportGatherer.gatherImports(type);
    Set<Import> imports = new LinkedHashSet<>();
    Iterables.addAll(imports, Iterables.concat(importsByCategory.values()));
    Map<Variable, String> aliasByVariable =
        VariableAliasesGatheringVisitor.gatherVariableAliases(imports, type);
    environment = new GenerationEnvironment(imports, aliasByVariable);
  }

  public Map<SourcePosition, SourcePosition> getSourceMappings() {
    return sourceBuilder.getMappings();
  }

  abstract String renderOutput();

  abstract String getSuffix();

  void renderFileOverview(String... suppressions) {
    String transpiledFrom =
        type.getDescriptor()
            .getUnsafeTypeDescriptor()
            .getRawTypeDescriptor()
            .getQualifiedBinaryName();
    sourceBuilder.appendLines(
        "/**",
        " * @fileoverview transpiled from " + transpiledFrom + ".",
        " *",
        " * @suppress {" + Joiner.on(", ").join(suppressions) + "}",
        " */");
    sourceBuilder.newLine();
  }

  static Iterable<Import> sortImports(Iterable<Import> iterable) {
    List<Import> sortedList = new ArrayList<>();
    Iterables.addAll(sortedList, iterable);
    Collections.sort(sortedList);
    return sortedList;
  }
}
