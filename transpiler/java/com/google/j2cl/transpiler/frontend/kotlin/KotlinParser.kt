/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.transpiler.frontend.kotlin

import com.google.j2cl.common.Problems
import com.google.j2cl.common.SourceUtils
import com.google.j2cl.transpiler.ast.CompilationUnit
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.EXCEPTION
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices

/** A parser for Kotlin sources that builds {@code CompilationtUnit}s. */
class KotlinParser(private val classpathEntries: List<String>, private val problems: Problems) {

  /** Returns a list of compilation units after Kotlinc parsing. */
  fun parseFiles(filePaths: List<SourceUtils.FileInfo>): List<CompilationUnit> {
    if (filePaths.isEmpty()) {
      return emptyList()
    }

    val compilerConfiguration = createCompilerConfiguration(problems)
    val environment =
      KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable(),
        compilerConfiguration,
        EnvironmentConfigFiles.JVM_CONFIG_FILES
      )
    environment.addKotlinSourceRoots(filePaths.map { f -> File(f.originalPath()) })

    val project =
      PsiBasedProjectEnvironment(
        environment.project,
        VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
      ) { environment.createPackagePartProvider(it) }

    return FirAnalyzerFacade(
        createFirSession(project, environment),
        environment.configuration.languageVersionSettings,
        environment.getSourceFiles()
      )
      .convertToIr(JvmGeneratorExtensionsImpl(compilerConfiguration))
      .getCompilationUnits()
  }

  private fun createFirSession(
    project: PsiBasedProjectEnvironment,
    environment: KotlinCoreEnvironment
  ): FirSession {
    val compilerConfiguration = environment.configuration

    return FirSessionFactory.createSessionWithDependencies(
      Name.identifier(compilerConfiguration.getNotNull(CommonConfigurationKeys.MODULE_NAME)),
      JvmPlatforms.unspecifiedJvmPlatform,
      JvmPlatformAnalyzerServices,
      /* externalSessionProvider */ null,
      project,
      compilerConfiguration.languageVersionSettings,
      project.getSearchScopeByPsiFiles(environment.getSourceFiles()),
      project.getSearchScopeForProjectLibraries(),
      compilerConfiguration.get(CommonConfigurationKeys.LOOKUP_TRACKER),
      /* providerAndScopeForIncrementalCompilation */ null,
      {
        dependencies(compilerConfiguration.jvmClasspathRoots.map { it.toPath() })
        dependencies(compilerConfiguration.jvmModularRoots.map { it.toPath() })
      }
    )
  }

  private fun createCompilerConfiguration(problems: Problems): CompilerConfiguration {
    val compilerConfiguration = CompilerConfiguration()
    compilerConfiguration.put(
      CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
      GroupingMessageCollector(ProblemsMessageCollector(problems), false)
    )
    compilerConfiguration.put(
      CommonConfigurationKeys.MODULE_NAME,
      JvmProtoBufUtil.DEFAULT_MODULE_NAME
    )
    compilerConfiguration.addJvmClasspathRoots(classpathEntries.map { f -> File(f) })
    return compilerConfiguration
  }

  private class ProblemsMessageCollector constructor(private val problems: Problems) :
    MessageCollector {
    override fun clear() {
      // This implementation do not support clearing error messages.
    }

    override fun hasErrors(): Boolean {
      return problems.hasErrors()
    }

    override fun report(
      severity: CompilerMessageSeverity,
      message: String,
      location: CompilerMessageSourceLocation?
    ) {
      if (severity != ERROR && severity != EXCEPTION) {
        return
      }

      if (location != null) {
        problems.error(location.line, location.path, "%s", message)
      } else {
        problems.error("%s", message)
      }
    }
  }
}
