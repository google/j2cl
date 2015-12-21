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
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor.ImportCategory;
import com.google.j2cl.generator.visitors.ImportUtils;
import com.google.j2cl.generator.visitors.VariableAliasesGatheringVisitor;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A base class for JavaScript source generators. We may have two subclasses, which are
 * for Header and Impl generation.
 */
public abstract class JavaScriptGenerator extends AbstractSourceGenerator {
  protected final JavaType javaType;
  protected final VelocityEngine velocityEngine;
  protected SourceGenerator sourceGenerator;
  protected GenerationEnvironment environment;

  public JavaScriptGenerator(Errors errors, JavaType javaType, VelocityEngine velocityEngine) {
    super(errors);
    this.javaType = javaType;
    this.velocityEngine = velocityEngine;
  }

  @Override
  public String toSource() {
    VelocityContext velocityContext = createContext();
    StringWriter outputBuffer = new StringWriter();

    boolean success =
        velocityEngine.mergeTemplate(
            getTemplateFilePath(), StandardCharsets.UTF_8.name(), velocityContext, outputBuffer);

    if (!success) {
      errors.error(Errors.Error.ERR_CANNOT_GENERATE_OUTPUT);
      return "";
    }
    return outputBuffer.toString();
  }

  protected VelocityContext createContext() {
    VelocityContext context = new VelocityContext();

    Map<ImportCategory, Set<Import>> importsByCategory =
        ImportGatheringVisitor.gatherImports(javaType);

    List<Import> sortedImports = ImportUtils.getSortedImports(importsByCategory);

    Map<Variable, String> aliasByVariable =
        VariableAliasesGatheringVisitor.gatherVariableAliases(sortedImports, javaType);

    environment = new GenerationEnvironment(sortedImports, aliasByVariable);
    sourceGenerator = new SourceGenerator(environment);

    TypeDescriptor selfTypeDescriptor = javaType.getDescriptor().getRawTypeDescriptor();
    context.put("classType", javaType);
    context.put("selfImport", new Import(selfTypeDescriptor.getSimpleName(), selfTypeDescriptor));
    context.put("GeneratorUtils", GeneratorUtils.class);
    context.put("ManglingNameUtils", ManglingNameUtils.class);
    context.put(
        "eagerImports", ImportUtils.sortedList(importsByCategory.get(ImportCategory.EAGER)));
    context.put("lazyImports", ImportUtils.sortedList(importsByCategory.get(ImportCategory.LAZY)));
    context.put("sourceGenerator", sourceGenerator);
    context.put(
        "javaLangClassTypeDecriptor", TypeDescriptors.get().javaLangClass.getRawTypeDescriptor());
    context.put("nativeUtilTypeDecriptor", BootstrapType.NATIVE_UTIL.getDescriptor());
    return context;
  }
}
