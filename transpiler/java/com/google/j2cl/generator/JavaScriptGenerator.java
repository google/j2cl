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

import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.errors.Errors;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Generates JavaScript source files.
 */
public class JavaScriptGenerator extends AbstractSourceGenerator {
  private static final String CLASSPATH_RESOURCE_LOADER_CLASS = "classpath.resource.loader.class";
  private static final String TEMPLATE_FILE_PATH = "com/google/j2cl/generator/JsCompilationUnit.vm";
  private final CompilationUnit compilationUnit;

  public JavaScriptGenerator(
      Errors errors,
      String outputPath,
      File outputDirectory,
      Charset charset,
      CompilationUnit compilationUnit) {
    super(errors, outputPath, outputDirectory, charset);
    this.compilationUnit = compilationUnit;
  }

  @Override
  public String toSource() {
    VelocityEngine velocityEngine = createEngine();
    VelocityContext velocityContext = createContext();
    StringWriter outputBuffer = new StringWriter();

    boolean success =
        velocityEngine.mergeTemplate(
            TEMPLATE_FILE_PATH, StandardCharsets.UTF_8.name(), velocityContext, outputBuffer);

    if (!success) {
      errors.error(Errors.ERR_CANNOT_GENERATE_OUTPUT);
      return "";
    }
    return outputBuffer.toString();
  }

  /**
   * Creates and returns a VelocityEngine that will find templates on the classpath.
   */
  private VelocityEngine createEngine() {
    VelocityEngine velocityEngine = new VelocityEngine();
    velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    velocityEngine.setProperty(
        CLASSPATH_RESOURCE_LOADER_CLASS, ClasspathResourceLoader.class.getName());
    velocityEngine.init();
    return velocityEngine;
  }

  private VelocityContext createContext() {
    VelocityContext context = new VelocityContext();

    // TODO: to be implemented.
    context.put("compilationUnit", compilationUnit);

    return context;
  }

  @Override
  public String getSuffix() {
    return ".js";
  }
}
