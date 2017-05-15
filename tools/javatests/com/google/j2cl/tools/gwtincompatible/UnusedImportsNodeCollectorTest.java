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
package com.google.j2cl.tools.gwtincompatible;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UnusedImportsNodeCollector}. TODO(stalcup): Consider moving this test to an
 * integration test.
 */
@RunWith(JUnit4.class)
public class UnusedImportsNodeCollectorTest {

  @Test
  public void testGetUnusedImports() {
    String source =
        Joiner.on("\n")
            .join(
                "package a.b.c;",
                "import x1.y1.z1.*;",
                "import x1.y2.z1.A;",
                "import x1.y2.z1.B;",
                "import x1.y2.z1.C;",
                "import x1.y2.z1.D;",
                "import x1.y2.z1.E;",
                "import x1.y2.z1.F;",
                "import x1.y2.z1.G;",
                "import x2.H;",
                "import x2.I;",
                "import static x2.L;",
                "import x2.M;",
                "/** See {@link M}.**/ ",
                "public class Foo extends A {",
                "  @I",
                "  public void m(B b) {",
                "    C c = new D();",
                "    return E.r() + F.e + L;",
                "  }",
                "}");
    List<String> imports = getUnusedImports(source);
    assertEquals(Lists.newArrayList("x1.y2.z1.G", "x2.H", "x2.M"), imports);
  }

  private List<String> getUnusedImports(String source) {
    ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setSource(source.toCharArray());
    CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);

    UnusedImportsNodeCollector importsCollector = new UnusedImportsNodeCollector();
    compilationUnit.accept(importsCollector);
    List<ImportDeclaration> unusedImportsDeclarations = importsCollector.getUnusedImports();
    List<String> unusedImports = new ArrayList<>();
    for (ImportDeclaration importDeclaration : unusedImportsDeclarations) {
      unusedImports.add(importDeclaration.getName().toString());
    }
    return unusedImports;
  }
}
