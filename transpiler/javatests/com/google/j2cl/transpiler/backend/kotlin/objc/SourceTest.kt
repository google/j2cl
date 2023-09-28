/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.backend.kotlin.objc

import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.rendererOf
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.testing.assertBuilds
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SourceTest {
  @Test
  fun sourceComment() {
    comment(source("A comment...")).assertBuilds("// A comment...")
  }

  @Test
  fun sourceSemicolonEnded() {
    semicolonEnded(source("foo")).assertBuilds("foo;")
  }

  @Test
  fun sourceAssignment() {
    assignment(source("foo"), source("bar")).assertBuilds("foo = bar")
  }

  @Test
  fun sourceParameter() {
    parameter(source("name"), source("value")).assertBuilds("name:value")
  }

  @Test
  fun sourcePointer() {
    pointer(source("foo")).assertBuilds("foo*")
  }

  @Test
  fun sourceMacroDeclaration() {
    macroDeclaration(source("foo")).assertBuilds("#foo")
  }

  @Test
  fun sourceMacroDefine() {
    macroDefine(source("foo")).assertBuilds("#define foo")
  }

  @Test
  fun sourceCompatibilityAlias() {
    compatibilityAlias(source("alias"), source("target"))
      .assertBuilds("@compatibility_alias alias target")
  }

  @Test
  fun sourceDefineAlias() {
    defineAlias(source("alias"), source("target")).assertBuilds("#define alias target")
  }

  @Test
  fun localImportSource() {
    localImport("local.h").source.assertBuilds("""#import "local.h"""")
  }

  @Test
  fun systemImportSource() {
    systemImport("system.h").source.assertBuilds("""#import <system.h>""")
  }

  @Test
  fun classForwardDeclarationSource() {
    classForwardDeclaration("Foo").source.assertBuilds("@class Foo;")
  }

  @Test
  fun protocolForwardDeclarationSource() {
    protocolForwardDeclaration("Foo").source.assertBuilds("@protocol Foo;")
  }

  @Test
  fun declarationsSource() {
    val dependencies =
      setOf(
        dependency(localImport("local_2.h")),
        dependency(systemImport("system_2.h")),
        dependency(classForwardDeclaration("Class2")),
        dependency(protocolForwardDeclaration("Protocol2")),
        dependency(localImport("local_1.h")),
        dependency(systemImport("system_1.h")),
        dependency(classForwardDeclaration("Class1")),
        dependency(protocolForwardDeclaration("Protocol1"))
      )

    dependenciesSource(dependencies)
      .assertBuilds(
        """
        #import <system_1.h>
        #import <system_2.h>

        #import "local_1.h"
        #import "local_2.h"

        @class Class1;
        @class Class2;

        @protocol Protocol1;
        @protocol Protocol2;
        """
          .trimIndent()
      )
  }

  @Test
  fun rendererSourceWithDependencies() {
    rendererOf(source("void main() {}"))
      .plus(dependency(systemImport("std.h")))
      .sourceWithDependencies
      .assertBuilds(
        """
      #import <std.h>

      void main() {}
      """
          .trimIndent()
      )
  }
}
