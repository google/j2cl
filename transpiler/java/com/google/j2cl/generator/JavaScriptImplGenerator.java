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

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor.ImportCategory;
import com.google.j2cl.generator.visitors.ImportUtils;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Generates JavaScript source impl files.
 */
public class JavaScriptImplGenerator extends JavaScriptGenerator {
  private VelocityContext velocityContext;
  private String nativeSource;
  private String relativeSourceMapLocation;
  private SourceBuilder sb;

  public JavaScriptImplGenerator(Errors errors, JavaType javaType, VelocityEngine velocityEngine) {
    super(errors, javaType, velocityEngine);
    velocityContext = createContext();
  }

  @Override
  public String getTemplateFilePath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getSuffix() {
    return ".impl.js";
  }

  public void setRelativeSourceMapLocation(String relativeSourceMapLocation) {
    Preconditions.checkArgument(relativeSourceMapLocation != null);
    this.relativeSourceMapLocation = relativeSourceMapLocation;
  }

  public void setNativeSource(String nativeSource) {
    Preconditions.checkArgument(nativeSource != null);
    this.nativeSource = nativeSource;
  }

  private String renderTemplate(String templatePath) {
    StringWriter outputBuffer = new StringWriter();
    boolean success =
        velocityEngine.mergeTemplate(
            templatePath, StandardCharsets.UTF_8.name(), velocityContext, outputBuffer);
    if (!success) {
      errors.error(Errors.Error.ERR_CANNOT_GENERATE_OUTPUT);
      return "";
    }
    return outputBuffer.toString();
  }

  @Override
  public String toSource() {
    sb = new SourceBuilder();
    renderFileOverview();
    renderImports();
    renderTypeBody();
    renderNativeSource();
    renderExports();
    renderSourceMapLocation();
    return sb.build();
  }

  private void renderFileOverview() {
    String transpiledFrom = javaType.getDescriptor().getRawTypeDescriptor().getSourceName();
    sb.appendln("/**");
    sb.appendln(" * @fileoverview Impl transpiled from " + transpiledFrom + ".");
    sb.appendln(" *");
    sb.appendln(
        " * @suppress {suspiciousCode, transitionalSuspiciousCodeWarnings, uselessCode, const}");
    sb.appendln(" */");
  }

  private void renderImports() {
    TypeDescriptor selfTypeDescriptor = javaType.getDescriptor().getRawTypeDescriptor();
    Import selfImport = new Import(selfTypeDescriptor.getSimpleName(), selfTypeDescriptor);

    // goog.module(...) declaration.
    sb.appendln("goog.module('%s');", selfImport.getImplModulePath());
    sb.newLine();
    sb.newLine();

    // goog.require(...) for eager imports.
    for (Import eagerImport : ImportUtils.sortedList(importsByCategory.get(ImportCategory.EAGER))) {
      String alias = eagerImport.getAlias();
      String path = eagerImport.getImplModulePath();
      sb.appendln("let %s = goog.require('%s');", alias, path);
    }
    sb.newLine();

    // goog.forwardDeclare(...) for lazy imports.
    for (Import lazyImport : ImportUtils.sortedList(importsByCategory.get(ImportCategory.LAZY))) {
      String alias = lazyImport.getAlias();
      String path = lazyImport.getImplModulePath();
      sb.appendln("let %s = goog.forwardDeclare('%s');", alias, path);
    }
    sb.newLine();
    sb.newLine();
  }

  private void renderJavaTypeMethods() {
    for (Method method : javaType.getMethods()) {
      velocityContext.put("method", method);
      if (method.isConstructor()) {
        sb.append(renderTemplate("com/google/j2cl/generator/JsConstructorMethods.vm"));
      } else {
        if (GeneratorUtils.shouldNotEmitCode(method)) {
          continue;
        }
        if (method.getDescriptor().isJsOverlay() && !method.getDescriptor().isStatic()) {
          continue;
        }
        sb.appendln("/**");
        if (method.isSynthetic()) {
          sb.appendln(" * Synthetic method.");
        }
        if (method.isAbstract()) {
          sb.appendln(" * Abstract method.");
        }
        if (method.isOverride()) {
          sb.appendln(" * @override");
        }
        if (!method.getDescriptor().getTypeParameterTypeDescriptors().isEmpty()) {
          String templateParamNames =
              sourceGenerator.getJsDocNames(
                  method.getDescriptor().getTypeParameterTypeDescriptors());
          sb.appendln(" * @template %s", templateParamNames);
        }
        for (String paramTypeName :
            GeneratorUtils.getParameterAnnotationsJsDoc(method, sourceGenerator)) {
          sb.appendln(" * @param %s", paramTypeName);
        }
        if (!GeneratorUtils.isVoid(method.getDescriptor().getReturnTypeDescriptor())) {
          String returnTypeName =
              sourceGenerator.getJsDocName(method.getDescriptor().getReturnTypeDescriptor());
          sb.appendln(" * @return {%s}", returnTypeName);
        }
        sb.appendln(" * @%s", method.getDescriptor().getVisibility().jsText);
        sb.appendln(" */");
        if (!method.isNative()) {
          sb.appendln(GeneratorUtils.getMethodHeader(method, sourceGenerator));
          StatementTransformer.transform(method.getBody(), environment, sb);
        } else {
          sb.appendln("  // native " + GeneratorUtils.getMethodHeader(method, sourceGenerator));
        }
        sb.newLine();
      }
    }
  }

  private void renderTypeBody() {
    if (javaType.isJsOverlayImpl()) {
      renderJsNativeTypeSource();
    } else if (javaType.isInterface()) {
      sb.append(renderTemplate("com/google/j2cl/generator/JsInterface.vm"));
      renderJavaTypeMethods();
      sb.append(renderTemplate("com/google/j2cl/generator/JsInterfaceSuffix.vm"));
    } else { // Not an interface so it is a Class.
      sb.append(renderTemplate("com/google/j2cl/generator/JsClass.vm"));
      renderJavaTypeMethods();
      sb.append(renderTemplate("com/google/j2cl/generator/JsClassSuffix.vm"));
    }
  }

  private void renderJsNativeTypeSource() {
    String className = sourceGenerator.toSource(javaType.getDescriptor());
    sb.appendln("class %s {", className);
    renderJavaTypeMethods();
    sb.append(renderTemplate("com/google/j2cl/generator/JsNativeTypeImplSuffix.vm"));
  }

  private void renderNativeSource() {
    if (nativeSource != null) {
      sb.newLine();
      sb.appendln("/**");
      sb.appendln(" * Native Method Injection");
      sb.appendln(" */");
      sb.appendln(nativeSource);
    }
  }

  private void renderExports() {
    sb.newLine();
    sb.appendln("/**");
    sb.appendln(" * Export class.");
    sb.appendln(" */");
    String className = sourceGenerator.toSource(javaType.getDescriptor());
    sb.appendln("exports = %s;", className);
  }

  private void renderSourceMapLocation() {
    if (relativeSourceMapLocation != null) {
      sb.append(String.format("//# sourceMappingURL=%s", relativeSourceMapLocation));
    }
  }
}
