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

import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor.ImportCategory;
import com.google.j2cl.generator.visitors.ImportUtils;
import com.google.j2cl.generator.visitors.VariableAliasesGatheringVisitor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A base class for JavaScript source generators. We may have two subclasses, which are
 * for Header and Impl generation.
 */
public abstract class JavaScriptGenerator {
  protected final JavaType javaType;
  protected GenerationEnvironment environment;
  protected Map<ImportCategory, Set<Import>> importsByCategory;
  protected final SourceBuilder sourceBuilder = new SourceBuilder();
  protected final Errors errors;
  protected final boolean declareLegacyNamespace;

  public JavaScriptGenerator(Errors errors, boolean declareLegacyNamespace, JavaType javaType) {
    this.errors = errors;
    this.declareLegacyNamespace = declareLegacyNamespace;
    this.javaType = javaType;
    importsByCategory = ImportGatheringVisitor.gatherImports(javaType);
    List<Import> sortedImports = ImportUtils.getSortedImports(importsByCategory);
    Map<Variable, String> aliasByVariable =
        VariableAliasesGatheringVisitor.gatherVariableAliases(sortedImports, javaType);
    environment = new GenerationEnvironment(sortedImports, aliasByVariable);
  }

  abstract String renderOutput();

  abstract String getSuffix();
}
