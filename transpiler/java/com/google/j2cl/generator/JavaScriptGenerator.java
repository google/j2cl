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

import com.google.common.collect.Lists;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor.ImportCategory;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A base class for JavaScript source generators. We may have two subclasses, which are
 * for Header and Impl generation.
 */
public abstract class JavaScriptGenerator extends AbstractSourceGenerator {
  private final JavaType javaType;
  protected final VelocityEngine velocityEngine;

  public JavaScriptGenerator(
      Errors errors,
      FileSystem outputFileSystem,
      String outputLocationPath,
      Charset charset,
      JavaType javaType,
      VelocityEngine velocityEngine) {
    super(errors, outputFileSystem, outputLocationPath, createRelativeFilePath(javaType), charset);
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
      errors.error(Errors.ERR_CANNOT_GENERATE_OUTPUT);
      return "";
    }
    return outputBuffer.toString();
  }

  protected VelocityContext createContext() {
    VelocityContext context = new VelocityContext();

    Map<ImportCategory, Set<Import>> importsByCategory =
        ImportGatheringVisitor.gatherImports(javaType);

    StatementSourceGenerator statementSourceGenerator =
        new StatementSourceGenerator(
            sortedList(
                union(
                    importsByCategory.get(ImportCategory.EAGER),
                    importsByCategory.get(ImportCategory.LAZY))));

    TypeDescriptor selfTypeDescriptor = javaType.getDescriptor().getRawTypeDescriptor();
    context.put("classType", javaType);
    context.put("selfImport", new Import(selfTypeDescriptor.getSimpleName(), selfTypeDescriptor));
    context.put("TranspilerUtils", TranspilerUtils.class);
    context.put("ManglingNameUtils", ManglingNameUtils.class);
    context.put("eagerImports", sortedList(importsByCategory.get(ImportCategory.EAGER)));
    context.put("lazyImports", sortedList(importsByCategory.get(ImportCategory.LAZY)));
    context.put("statementSourceGenerator", statementSourceGenerator);
    context.put("javaLangClassTypeDecriptor", TypeDescriptors.CLASS_TYPE_DESCRIPTOR);
    context.put("nativeUtilTypeDecriptor", TypeDescriptors.NATIVE_UTIL_TYPE_DESCRIPTOR);

    return context;
  }

  private static <T extends Comparable<T>> List<T> sortedList(Set<T> set) {
    List<T> sortedList = Lists.newArrayList(set);
    Collections.sort(sortedList);
    return sortedList;
  }

  private static <T> Set<T> union(Set<T> left, Set<T> right) {
    HashSet<T> union = new HashSet<>();
    union.addAll(left);
    union.addAll(right);
    return union;
  }

  private static String createRelativeFilePath(JavaType javaType) {
    return TranspilerUtils.getOutputPath(javaType);
  }
}
