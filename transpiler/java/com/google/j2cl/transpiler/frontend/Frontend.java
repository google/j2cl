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

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.frontend.common.FrontendOptions;
import com.google.j2cl.transpiler.frontend.javac.JavacParser;
import com.google.j2cl.transpiler.frontend.jdt.CompilationUnitBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/** Drives the frontend to parse, type check and resolve Java source code. */
public enum Frontend {
  JDT {
    @Override
    public Library parse(FrontendOptions options, Problems problems) {
      // TODO(goktug): Create a frontend entry point consistent with the other frontends.
      return Library.newBuilder()
          .setCompilationUnits(CompilationUnitBuilder.build(options, problems))
          .build();
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
      try {
        // Temporary workaround to turn Kotlin compiler dep into a soft runtime dependency.
        // TODO(b/217287994): Remove after a regular dependency is allowed.
        Class<?> kotlinParser =
            Class.forName("com.google.j2cl.transpiler.frontend.kotlin.KotlinParser");
        Constructor<?> parserCtor =
            Iterables.getOnlyElement(Arrays.asList(kotlinParser.getDeclaredConstructors()));
        return (Library)
            kotlinParser
                .getMethod("parseFiles", FrontendOptions.class)
                .invoke(parserCtor.newInstance(problems), options);
      } catch (Exception e) {
        // Retrieve the original exception if it was thrown by the method called using invoke.
        Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
        Throwables.throwIfUnchecked(cause);
        throw new RuntimeException(cause);
      }
    }

    @Override
    public boolean isJavaFrontend() {
      return false;
    }
  };

  public abstract Library parse(FrontendOptions options, Problems problems);

  public abstract boolean isJavaFrontend();
}
