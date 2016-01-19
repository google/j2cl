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
import com.google.j2cl.errors.Errors;

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
  private SourceBuilder sourceBuilder;

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
    sourceBuilder = new SourceBuilder();
    sourceBuilder.append(renderTemplate("com/google/j2cl/generator/JsImports.vm"));
    renderTypeBody();
    renderNativeSource();
    renderExports();
    renderSourceMapLocation();
    return sourceBuilder.build();
  }

  private void renderSourceMapLocation() {
    if (relativeSourceMapLocation != null) {
      sourceBuilder.append(String.format("//# sourceMappingURL=%s", relativeSourceMapLocation));
    }
  }

  private void renderJavaTypeMethods() {
    for (Method method : javaType.getMethods()) {
      velocityContext.put("method", method);
      if (method.isConstructor()) {
        sourceBuilder.append(renderTemplate("com/google/j2cl/generator/JsConstructorMethods.vm"));
      } else {
        sourceBuilder.append(renderTemplate("com/google/j2cl/generator/JsMethodHeader.vm"));
        if (!method.isNative()) {
          StatementTransformer.transform(method.getBody(), environment, sourceBuilder);
          sourceBuilder.append("\n");
        }
      }
    }
  }

  private void renderTypeBody() {
    if (javaType.isJsOverlayImpl()) {
      renderJsNativeTypeSource();
    } else if (javaType.isInterface()) {
      sourceBuilder.append(renderTemplate("com/google/j2cl/generator/JsInterface.vm"));
      renderJavaTypeMethods();
      sourceBuilder.append(renderTemplate("com/google/j2cl/generator/JsInterfaceSuffix.vm"));
    } else { // Not an interface so it is a Class.
      sourceBuilder.append(renderTemplate("com/google/j2cl/generator/JsClass.vm"));
      renderJavaTypeMethods();
      sourceBuilder.append(renderTemplate("com/google/j2cl/generator/JsClassSuffix.vm"));
    }
  }

  private void renderJsNativeTypeSource() {
    String className = sourceGenerator.toSource(javaType.getDescriptor());
    sourceBuilder.appendln(String.format("class %s {", className));
    for (Method method : javaType.getMethods()) {
      if (method.getDescriptor().isJsOverlay() && method.getDescriptor().isStatic()) {
        velocityContext.put("method", method);
        sourceBuilder.append(renderTemplate("com/google/j2cl/generator/JsMethodHeader.vm"));
        sourceBuilder.appendln(sourceGenerator.toSource(method.getBody()));
      }
    }
    sourceBuilder.append(renderTemplate("com/google/j2cl/generator/JsNativeTypeImplSuffix.vm"));
  }

  private void renderNativeSource() {
    if (nativeSource != null) {
      sourceBuilder.appendln(""); // New line before
      sourceBuilder.appendln("/**");
      sourceBuilder.appendln(" * Native Method Injection");
      sourceBuilder.appendln(" */");
      sourceBuilder.appendln(nativeSource);
    }
  }

  private void renderExports() {
    sourceBuilder.appendln(""); // New line before
    sourceBuilder.appendln("/**");
    sourceBuilder.appendln(" * Export class.");
    sourceBuilder.appendln(" */");
    String className = sourceGenerator.toSource(javaType.getDescriptor());
    sourceBuilder.appendln(String.format("exports = %s;", className));
  }
}
