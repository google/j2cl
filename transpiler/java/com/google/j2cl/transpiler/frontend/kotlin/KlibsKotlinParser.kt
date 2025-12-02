/*
 * Copyright 2025 Google Inc.
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
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin

import com.google.j2cl.common.Problems
import com.google.j2cl.common.SourceUtils
import com.google.j2cl.common.SourceUtils.FileInfo
import com.google.j2cl.transpiler.ast.CompilationUnit
import com.google.j2cl.transpiler.ast.Library
import com.google.j2cl.transpiler.frontend.common.FrontendOptions
import com.google.j2cl.transpiler.frontend.kotlin.ir.IntrinsicMethods
import com.google.j2cl.transpiler.frontend.kotlin.jklib.K2JKlibCompiler
import com.google.j2cl.transpiler.frontend.kotlin.jklib.K2JKlibCompilerArguments
import com.google.j2cl.transpiler.frontend.kotlin.lower.LoweringPasses
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import java.io.File
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.setupCommonArguments
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME
import org.jetbrains.kotlin.config.CommonConfigurationKeys.USE_FIR
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.util.PerformanceManager

/** A parser for Kotlin sources that builds {@code CompilationtUnit}s. */
class KlibsKotlinParser(private val problems: Problems) {

  /** Returns a list of compilation units after Kotlinc parsing. */
  fun parseFiles(options: FrontendOptions): Library {
    check(options.enableKlibs) { "This parser only supports Klibs" }

    val arguments = createCompilerArguments(options)
    val compilerConfiguration = createCompilerConfiguration(arguments, options)
    problems.abortIfCancelled()

    check(compilerConfiguration.getBoolean(USE_FIR)) { "Kotlin/Closure only supports > K2" }

    val kotlincDisposable = Disposer.newDisposable("J2CL Root Disposable")
    try {
      problems.registerForCancellation()
      val compilationUnits =
        parseFiles(arguments, compilerConfiguration, kotlincDisposable, options.targetLabel)

      return Library.newBuilder()
        .setCompilationUnits(compilationUnits)
        .setDisposableListener { Disposer.dispose(kotlincDisposable) }
        .build()
    } catch (e: Throwable) {
      // Clean up disposable before exiting to avoid memory leaks.
      Disposer.dispose(kotlincDisposable)

      // TODO(b/460684524): Remove this once we have a better way to handle compilation errors.
      if (e is CompilationErrorException) {
        problems.abortIfHasErrors()
      }
      throw e
    }
  }

  private fun parseFiles(
    arguments: K2JKlibCompilerArguments,
    compilerConfiguration: CompilerConfiguration,
    disposable: Disposable,
    currentTarget: String?,
  ): List<CompilationUnit> {

    val messageCollector = compilerConfiguration.get(MESSAGE_COLLECTOR_KEY)!!
    val projectEnvironment =
      createProjectEnvironment(
        compilerConfiguration,
        disposable,
        EnvironmentConfigFiles.JVM_CONFIG_FILES,
        messageCollector,
      )
    problems.abortIfCancelled()

    val compilationResult =
      K2JKlibCompiler()
        .compileKlibAndDeserializeIr(arguments, compilerConfiguration, disposable, null)
    problems.abortIfCancelled()

    val moduleFragment = compilationResult.mainModuleFragment
    val pluginContext = compilationResult.pluginContext

    val state =
      GenerationState(
        projectEnvironment.project,
        moduleFragment.descriptor,
        compilerConfiguration,
        targetId = TargetId(compilerConfiguration.get(MODULE_NAME)!!, "java-production"),
        diagnosticReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector),
      )

    val lowerings = LoweringPasses(state, compilerConfiguration)
    lowerings.generate(moduleFragment, pluginContext)
    problems.abortIfCancelled()

    val jarFileSystem =
      KotlinCoreEnvironment.createForProduction(
          disposable,
          compilerConfiguration,
          EnvironmentConfigFiles.JVM_CONFIG_FILES,
        )
        .projectEnvironment
        .jarFileSystem
    val classpath =
      compilerConfiguration.jvmClasspathRoots.map { jarFileSystem.findFileByPath("$it!/")!! }

    val packageInfoCache = PackageInfoCache(classpath)
    problems.abortIfCancelled()

    val compilationUnits =
      CompilationUnitBuilder(
          KotlinEnvironment(pluginContext, packageInfoCache, lowerings.jvmBackendContext),
          IntrinsicMethods(pluginContext),
        )
        .convert(moduleFragment)
    problems.abortIfCancelled()

    return compilationUnits
  }

  fun createCompilerArguments(options: FrontendOptions): K2JKlibCompilerArguments {
    return K2JKlibCompilerArguments().also { arguments ->
      parseCommandLineArguments(options.kotlincOptions, arguments)
      arguments.classpath = options.classpaths.joinToString(File.pathSeparator)
      arguments.klibLibraries = options.dependencyKlibs.joinToString(File.pathSeparator)
      arguments.friendModules = options.friendKlibs.joinToString(File.pathSeparator)
      arguments.commonSources =
        options.sources
          .filter { it.originalPath().startsWith("common-srcs/") }
          .map(FileInfo::sourcePath)
          .toTypedArray()
      arguments.freeArgs = options.sources.map(SourceUtils.FileInfo::sourcePath)
    }
  }

  fun createCompilerConfiguration(
    arguments: K2JKlibCompilerArguments,
    options: FrontendOptions,
  ): CompilerConfiguration {
    val configuration = CompilerConfiguration()

    val messageCollector = problems.createMessageCollector()
    configuration.put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
    configuration.put(CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY, messageCollector)
    configuration.put(
      CLIConfigurationKeys.PERF_MANAGER,
      object : PerformanceManager(JvmPlatforms.defaultJvmPlatform, "J2clKotlinParser") {},
    )

    configuration.setupCommonArguments(arguments) { versionArray -> MetadataVersion(*versionArray) }

    return configuration
  }
}
