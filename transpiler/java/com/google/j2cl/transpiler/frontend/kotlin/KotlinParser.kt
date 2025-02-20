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
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin

import com.google.j2cl.common.Problems
import com.google.j2cl.common.SourceUtils.FileInfo
import com.google.j2cl.transpiler.ast.CompilationUnit
import com.google.j2cl.transpiler.ast.Library
import com.google.j2cl.transpiler.frontend.common.FrontendOptions
import com.google.j2cl.transpiler.frontend.common.PackageInfoCache
import com.google.j2cl.transpiler.frontend.jdt.PackageAnnotationsResolver
import com.google.j2cl.transpiler.frontend.kotlin.ir.IntrinsicMethods
import com.google.j2cl.transpiler.frontend.kotlin.ir.JvmIrDeserializerImpl
import com.google.j2cl.transpiler.frontend.kotlin.lower.LoweringPasses
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.io.File
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.collectSources
import org.jetbrains.kotlin.cli.common.computeKotlinPaths
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.setupCommonArguments
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.configureSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.IncrementalCompilationApi
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.ModuleCompilerInput
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.compileModuleToAnalyzedFirViaLightTreeIncrementally
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.withModule
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.jvm.configureAdvancedJvmOptions
import org.jetbrains.kotlin.cli.jvm.configureJavaModulesContentRoots
import org.jetbrains.kotlin.cli.jvm.configureJdkHome
import org.jetbrains.kotlin.cli.jvm.configureKlibPaths
import org.jetbrains.kotlin.cli.jvm.configureModuleChunk
import org.jetbrains.kotlin.cli.jvm.configureStandardLibs
import org.jetbrains.kotlin.cli.jvm.setupJvmSpecificArguments
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME
import org.jetbrains.kotlin.config.CommonConfigurationKeys.USE_FIR
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys.MODULES
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.PendingDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus

/** A parser for Kotlin sources that builds {@code CompilationtUnit}s. */
class KotlinParser(private val problems: Problems) {

  /** Returns a list of compilation units after Kotlinc parsing. */
  fun parseFiles(options: FrontendOptions): Library {
    val packageInfoCache = PackageInfoCache(options.classpaths, problems)
    problems.abortIfCancelled()
    val packageAnnotationResolver = getPackageAnnotationResolver(options, packageInfoCache)
    problems.abortIfCancelled()

    val compilerConfiguration = createCompilerConfiguration(options, packageInfoCache)
    problems.abortIfCancelled()

    check(compilerConfiguration.getBoolean(USE_FIR)) { "Kotlin/Closure only supports > K2" }

    val kotlincDisposable = Disposer.newDisposable("J2CL Root Disposable")
    try {
      // TODO(b/148292139): This assumes single worker for given time and doesn't work with
      // multiplex workers.
      ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(
        object : CompilationCanceledStatus {
          override fun checkCanceled() {
            // throw CompilationCanceledException instead of our own which is properly handled by
            // kotlinc to gracefully exit from the compilation.
            if (problems.isCancelled) throw CompilationCanceledException()
          }
        }
      )

      val compilationUnits =
        parseFiles(compilerConfiguration, kotlincDisposable, packageAnnotationResolver)

      return Library.newBuilder()
        .setCompilationUnits(compilationUnits)
        .setDisposableListener { Disposer.dispose(kotlincDisposable) }
        .build()
    } catch (e: Throwable) {
      // Clean up disposable if we are not properly exiting to avoid memory leaks.
      Disposer.dispose(kotlincDisposable)
      throw e
    } finally {
      ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(null)
    }
  }

  private fun parseFiles(
    compilerConfiguration: CompilerConfiguration,
    disposable: Disposable,
    packageAnnotationResolver: PackageAnnotationsResolver,
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

    val module = compilerConfiguration.get(MODULES)!![0]
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)
    val sources = collectSources(compilerConfiguration, projectEnvironment, messageCollector)
    problems.abortIfCancelled()

    val analysisResults =
      @OptIn(IncrementalCompilationApi::class)
      compileModuleToAnalyzedFirViaLightTreeIncrementally(
        projectEnvironment,
        messageCollector,
        compilerConfiguration,
        ModuleCompilerInput(TargetId(module), sources, compilerConfiguration),
        diagnosticsReporter,
        incrementalExcludesScope = null,
      )

    diagnosticsReporter.maybeReportErrorsAndAbort(messageCollector, compilerConfiguration)

    val state =
      GenerationState.Builder(
          projectEnvironment.project,
          ClassBuilderFactories.THROW_EXCEPTION,
          FirModuleDescriptor.createSourceModuleDescriptor(
            analysisResults.outputs[0].session,
            DefaultBuiltIns.Instance,
          ),
          NoScopeRecordCliBindingTrace(projectEnvironment.project).bindingContext,
          compilerConfiguration,
        )
        .withModule(module)
        .isIrBackend(true)
        .diagnosticReporter(diagnosticsReporter)
        .build()
    problems.abortIfCancelled()

    val compilationUnitBuilderExtension =
      createAndRegisterCompilationUnitBuilder(
        compilerConfiguration,
        projectEnvironment.project,
        state,
        packageAnnotationResolver,
      )
    problems.abortIfCancelled()

    val unused =
      analysisResults.convertToIrAndActualizeForJvm(
        JvmFir2IrExtensions(compilerConfiguration, JvmIrDeserializerImpl()),
        compilerConfiguration,
        diagnosticsReporter,
        IrGenerationExtension.getInstances(projectEnvironment.project),
      )

    diagnosticsReporter.maybeReportErrorsAndAbort(messageCollector, compilerConfiguration)

    return compilationUnitBuilderExtension.compilationUnits
  }

  private fun createAndRegisterCompilationUnitBuilder(
    compilerConfiguration: CompilerConfiguration,
    project: Project,
    state: GenerationState,
    packageAnnotationResolver: PackageAnnotationsResolver,
  ): CompilationUnitBuilderExtension {
    // Lower the IR tree before to convert it to a j2cl ast
    val lowerings = LoweringPasses(state, compilerConfiguration)
    IrGenerationExtension.registerExtension(project, lowerings)

    val compilationUnitBuilderExtension =
      object : IrGenerationExtension, CompilationUnitBuilderExtension {
        override lateinit var compilationUnits: List<CompilationUnit>

        override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
          compilationUnits =
            CompilationUnitBuilder(
                KotlinEnvironment(
                  pluginContext,
                  packageAnnotationResolver,
                  lowerings.jvmBackendContext,
                ),
                IntrinsicMethods(pluginContext.irBuiltIns),
              )
              .convert(moduleFragment)
        }
      }

    IrGenerationExtension.registerExtension(project, compilationUnitBuilderExtension)
    return compilationUnitBuilderExtension
  }

  private fun getPackageAnnotationResolver(
    options: FrontendOptions,
    packageInfoCache: PackageInfoCache,
  ): PackageAnnotationsResolver {
    val packageInfoSources: List<FileInfo> =
      options.sources.filter { it.originalPath().endsWith("package-info.java") }

    return PackageAnnotationsResolver.create(packageInfoSources, packageInfoCache, problems)
  }

  interface CompilationUnitBuilderExtension {
    val compilationUnits: List<CompilationUnit>
  }

  private fun createCompilerConfiguration(
    options: FrontendOptions,
    packageInfoCache: PackageInfoCache,
  ): CompilerConfiguration {
    val arguments = createCompilerArguments(options, packageInfoCache)
    val configuration = CompilerConfiguration()

    val messageCollector = ProblemsMessageCollector(problems)
    configuration.put(MESSAGE_COLLECTOR_KEY, messageCollector)
    configuration.put(ORIGINAL_MESSAGE_COLLECTOR_KEY, messageCollector)

    // Used by Kotlinc as name of the .kotlin_module file. This file will not be generated in our
    // case but this config item cannot be null. Currently only stdlib is passing a module name,
    // for others libraries we use the Kotlinc default name `main`
    configuration.put(MODULE_NAME, arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME)

    configuration.setupCommonArguments(arguments) { versionArray ->
      JvmMetadataVersion(*versionArray)
    }
    configuration.setupJvmSpecificArguments(arguments)
    configuration.configureJdkHome(arguments)
    configuration.configureJavaModulesContentRoots(arguments)
    configuration.configureStandardLibs(computeKotlinPaths(messageCollector, arguments), arguments)
    configuration.configureAdvancedJvmOptions(arguments)
    configuration.configureKlibPaths(arguments)

    // A build file can be passed to the compiler in order to define and compile several modules in
    // one compilation. The build file specifies which module each source file belongs to. The list
    // of modules are gathered in a ModuleChunk instance.
    // In our case, we only compile one module per compilation and `moduleChunk.modules` always
    // returns a list with only one module.
    val moduleChunk = configuration.configureModuleChunk(arguments, buildFile = null)
    configuration.configureSourceRoots(moduleChunk.modules, buildFile = null)

    configuration.configureJdkClasspathRoots()

    return configuration
  }

  private fun createCompilerArguments(
    options: FrontendOptions,
    packageInfoCache: PackageInfoCache,
  ) =
    K2JVMCompilerArguments().also { arguments ->
      parseCommandLineArguments(options.kotlincOptions, arguments)
      arguments.classpath = options.classpaths.joinToString(File.pathSeparator)
      arguments.commonSources =
        options.sources
          .filter { it.originalPath().startsWith("common-srcs/") }
          .map(FileInfo::sourcePath)
          .toTypedArray()
      arguments.freeArgs = options.sources.map(FileInfo::sourcePath)

      arguments.setEligibleFriends(packageInfoCache, options.targetLabel)
    }

  private fun PendingDiagnosticsCollectorWithSuppress.maybeReportErrorsAndAbort(
    messageCollector: MessageCollector,
    compilerConfiguration: CompilerConfiguration,
  ) {
    if (hasErrors) {
      reportToMessageCollector(
        messageCollector,
        compilerConfiguration.getBoolean(RENDER_DIAGNOSTIC_INTERNAL_NAME),
      )
    }
    problems.abortIfHasErrors()
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
      location: CompilerMessageSourceLocation?,
    ) {
      if (!severity.isError) {
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
