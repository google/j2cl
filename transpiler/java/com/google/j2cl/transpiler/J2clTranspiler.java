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
package com.google.j2cl.transpiler;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.kohsuke.args4j.CmdLineException;

import com.google.j2cl.errors.Errors;
import com.google.j2cl.frontend.FrontendOptions;
import com.google.j2cl.frontend.FrontendFlags;
import com.google.j2cl.frontend.JdtParser;

/**
 * Translation tool for generating JavaScript source files from Java sources.
 */
public class J2clTranspiler implements JdtParser.Handler {
  private final Errors errors;
  private final FrontendOptions options;

  public J2clTranspiler(FrontendOptions options, Errors errors) {
    this.options = options;
    this.errors = errors;
  }

  @Override
  public void handleCompilationUnit(String path, CompilationUnit unit) {
    System.out.println(path);
    System.out.println(unit);

    // transform JDT AST to J2cl AST

    // normalize J2cl AST

    // output J2cl AST as JS source.
  }

  /**
   * Runs the entire J2cl pipeline.
   */
  public void run() {
    JdtParser parser = new JdtParser(options, errors);
    parser.parseFiles(options.getSourceFiles(), this);
    errors.maybeReportAndExit();
  }

  /**
   * Entry point for the tool.
   * Parses options, creates a J2cl transpiler and calls run()
   */
  public static void main(String[] args) {
    Errors errors = new Errors();
    FrontendFlags flags = new FrontendFlags();
    try {
      flags.parse(args);
    } catch (CmdLineException e) {
      errors.error(Errors.ERR_INVALID_FLAG, e.getMessage());
    }
    errors.maybeReportAndExit();

    FrontendOptions options = new FrontendOptions(errors, flags);
    errors.maybeReportAndExit();

    J2clTranspiler transpiler = new J2clTranspiler(options, errors);
    transpiler.run();
  }
}
