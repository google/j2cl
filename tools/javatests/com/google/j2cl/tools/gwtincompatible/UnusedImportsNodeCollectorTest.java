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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UnusedImportsNodeCollector}. */
@RunWith(JUnit4.class)
public class UnusedImportsNodeCollectorTest {

  @Test
  public void testGetUnusedImports() throws Exception {
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

  private List<String> getUnusedImports(String source) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    SimpleJavaFileObject fileObject =
        new SimpleJavaFileObject(URI.create("string:///temp.java"), JavaFileObject.Kind.SOURCE) {
          @Override
          public String getCharContent(boolean ignoreEncodingErrors) {
            return source;
          }
        };
    JavacTask task =
        (JavacTask) compiler.getTask(null, null, null, null, null, ImmutableList.of(fileObject));
    UnusedImportsNodeCollector importsCollector = new UnusedImportsNodeCollector(new HashSet<>());
    importsCollector.scan(Iterables.getOnlyElement(task.parse()), null);
    return Lists.transform(
        importsCollector.getUnusedImports(), i -> i.getQualifiedIdentifier().toString());
  }
}
