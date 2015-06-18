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
package com.google.j2cl.generator.visitors;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.Visitor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

/**
 * Traverses a CompilationUnit and gathers imports for all its referenced types.
 */
// TODO: turn into an actual Visitor once we have such a framework.
public class ImportGatheringVisitor extends Visitor {
  private Set<Import> importModules = new LinkedHashSet<>();
  private Set<TypeReference> typeReferences = new LinkedHashSet<>();
  private Set<TypeReference> typeReferencesDefinedInCompilationUnit = new LinkedHashSet<>();

  public static Set<Import> gatherImports(CompilationUnit compilationUnit) {
    return new ImportGatheringVisitor().doGatherImports(compilationUnit);
  }

  @Override
  public void exitTypeReference(TypeReference typeReference) {
    typeReferences.add(typeReference);
  }

  @Override
  public void exitAssertStatement(AssertStatement assertStatement) {
    importModules.add(Import.IMPORT_ASSERTS);
  }

  @Override
  public void exitJavaType(JavaType type) {
    typeReferencesDefinedInCompilationUnit.add(type.getSelfReference());
  }

  private Set<Import> doGatherImports(CompilationUnit compilationUnit) {
    // Collect type references.
    compilationUnit.accept(this);
    Set<TypeReference> importTypeReferences =
        new TreeSet<>(Sets.difference(typeReferences, typeReferencesDefinedInCompilationUnit));
    importModules.addAll(
        FluentIterable.from(importTypeReferences)
            .transform(
                new Function<TypeReference, Import>() {
                  @Nullable
                  @Override
                  public Import apply(TypeReference typeReference) {
                    return new Import(typeReference);
                  }
                })
            .toList());
    return importModules;
  }

  private ImportGatheringVisitor() {}
}
