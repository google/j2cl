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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.sourcemaps.SourceInfo;

/**
 * Used for testing.
 */
public class SourceInfoPrinter {
  /**
   * The source location of the ast node to print, input or output.
   */
  public enum Type {
    INPUT,
    OUTPUT
  }

  public static void applyTo(CompilationUnit compilationUnit, Type type) {
    StaticFieldAccessGatherer gatherer = new StaticFieldAccessGatherer(type);
    compilationUnit.accept(gatherer);
  }

  private static class StaticFieldAccessGatherer extends AbstractVisitor {
    private Type type;

    private StaticFieldAccessGatherer(Type type) {
      this.type = type;
    }

    @Override
    public boolean enterStatement(Statement statement) {
      SourceInfo position =
          type == Type.INPUT ? statement.getInputSourceInfo() : statement.getOutputSourceInfo();
      System.out.printf(
          String.format(
              "%s line:%d col:%d -> line:%d col:%d%n",
              statement.getClass().getName(),
              position.getStartFilePosition().getLine(),
              position.getStartFilePosition().getColumn(),
              position.getEndFilePosition().getLine(),
              position.getEndFilePosition().getColumn()));
      return true;
    }
  }
}
