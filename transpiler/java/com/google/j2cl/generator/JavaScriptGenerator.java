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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;

/**
 * Generates JavaScript source files.
 */
public class JavaScriptGenerator extends AbstractSourceGenerator {
  public static final String TEMPLATE_FILE_NAME = "jsmodule.vm";
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
    // create an instance of Velocity Engine.
    VelocityEngine velocityEngine = new VelocityEngine();
    // initialize the engine
    velocityEngine.init();

    // create velocity context.
    VelocityContext velocityContext = createContext();

    Reader templateReader =
        new BufferedReader(
            new InputStreamReader(
                JavaScriptGenerator.class.getResourceAsStream(TEMPLATE_FILE_NAME),
                Charset.defaultCharset()));
    StringWriter writer = new StringWriter();
    // use the template file's name as log tag, which will be used as the template name for log
    // messages in case of error.
    String logTag = TEMPLATE_FILE_NAME.substring(0, TEMPLATE_FILE_NAME.length() - 3);
    // render the input template file in the form of InputeStream by {@code templateReader},
    // to an output Writer {@code writer}, using a Context {@code context}
    boolean renderResult = velocityEngine.evaluate(velocityContext, writer, logTag, templateReader);

    if (!renderResult) {
      errors.error(Errors.ERR_CANNOT_GENERATE_OUTPUT);
      return "";
    }
    return writer.toString();
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
