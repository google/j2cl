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
    Import.local("local.h").source.assertBuilds("""#import "local.h"""")
  }

  @Test
  fun systemImportSource() {
    Import.system("system.h").source.assertBuilds("""#import <system.h>""")
  }

  @Test
  fun classForwardDeclarationSource() {
    ForwardDeclaration.ofClass("Foo").source.assertBuilds("@class Foo;")
  }

  @Test
  fun protocolForwardDeclarationSource() {
    ForwardDeclaration.ofProtocol("Foo").source.assertBuilds("@protocol Foo;")
  }

  @Test
  fun declarationsSource() {
    val dependencies =
      setOf(
        Dependency.of(Import.local("local_2.h")),
        Dependency.of(Import.system("system_2.h")),
        Dependency.of(ForwardDeclaration.ofClass("Class2")),
        Dependency.of(ForwardDeclaration.ofProtocol("Protocol2")),
        Dependency.of(Import.local("local_1.h")),
        Dependency.of(Import.system("system_1.h")),
        Dependency.of(ForwardDeclaration.ofClass("Class1")),
        Dependency.of(ForwardDeclaration.ofProtocol("Protocol1"))
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
      .plus(Dependency.of(Import.system("std.h")))
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
