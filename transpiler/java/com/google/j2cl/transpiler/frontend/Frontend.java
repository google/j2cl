/*
 * Copyright 2019 Google Inc.
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
package com.google.j2cl.transpiler.frontend;

import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.frontend.common.FrontendOptions;
import com.google.j2cl.transpiler.frontend.javac.JavacParser;
import com.google.j2cl.transpiler.frontend.jdt.JdtParser;
import com.google.j2cl.transpiler.frontend.kotlin.KotlinParser;

/** Drives the frontend to parse, type check and resolve Java source code. */
public enum Frontend {
  JDT {
    @Override
    public Library parse(FrontendOptions options, Problems problems) {
      return new JdtParser(problems).parseFiles(options);
    }

    @Override
    public boolean isJavaFrontend() {
      return true;
    }
  },
  JAVAC {
    @Override
    public Library parse(FrontendOptions options, Problems problems) {
      return new JavacParser(problems).parseFiles(options);
    }

    @Override
    public boolean isJavaFrontend() {
      return true;
    }
  },
  KOTLIN {
    @Override
    public Library parse(FrontendOptions options, Problems problems) {
      return new KotlinParser(problems).parseFiles(options);
    }

    @Override
    public boolean isJavaFrontend() {
      return false;
    }
  };

  public abstract Library parse(FrontendOptions options, Problems problems);

  public abstract boolean isJavaFrontend();
}
