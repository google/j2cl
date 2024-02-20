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
import com.google.j2cl.transpiler.frontend.jdt.JdtParser
import com.google.j2cl.transpiler.frontend.jdt.PackageAnnotationsResolver
import com.google.j2cl.transpiler.frontend.kotlin.BazelJarAutoFriends.addEligibleFriends
import com.google.j2cl.transpiler.frontend.kotlin.lower.LoweringPasses
import com.intellij.openapi.util.Disposer
import java.io.File
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.computeKotlinPaths
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.IrMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.setupCommonArguments
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.configureSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.jvm.configureAdvancedJvmOptions
import org.jetbrains.kotlin.cli.jvm.configureJavaModulesContentRoots
import org.jetbrains.kotlin.cli.jvm.configureJdkHome
import org.jetbrains.kotlin.cli.jvm.configureKlibPaths
import org.jetbrains.kotlin.cli.jvm.configureModuleChunk
import org.jetbrains.kotlin.cli.jvm.configureStandardLibs
import org.jetbrains.kotlin.cli.jvm.setupJvmSpecificArguments
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil

/** A parser for Kotlin sources that builds {@code CompilationtUnit}s. */
class KotlinParser(
  private val classpathEntries: List<String>,
  private val kotlincopts: List<String>,
  private val problems: Problems,
  private val currentTarget: String?,
) {

  /** Returns a list of compilation units after Kotlinc parsing. */
  fun parseFiles(sources: List<FileInfo>): List<CompilationUnit> {
    if (sources.isEmpty()) {
      return emptyList()
    }

    val packageInfoSources: List<FileInfo> =
      sources.filter { it.originalPath().endsWith("package-info.java") }

    val packageAnnotationsResolver =
      PackageAnnotationsResolver.create(packageInfoSources, JdtParser(classpathEntries, problems))

    val environment =
      KotlinCoreEnvironment.createForProduction(
        // TODO(b/243860591): The disposable needs to be disposed once the transpilation is done.
        Disposer.newDisposable(),
        createCompilerConfiguration(sources),
        EnvironmentConfigFiles.JVM_CONFIG_FILES,
      )

    // Register friend modules so that we do not trigger visibility errors.
    ModuleVisibilityManager.SERVICE.getInstance(environment.project)
      .addEligibleFriends(currentTarget, classpathEntries)

    // analyze() will return null if it failed analysis phase. Errors should have been collected
    // into Problems.
    val analysis = KotlinToJVMBytecodeCompiler.analyze(environment) ?: return emptyList()

    val state =
      GenerationState.Builder(
          environment.project,
          ClassBuilderFactories.THROW_EXCEPTION,
          analysis.moduleDescriptor,
          analysis.bindingContext,
          environment.getSourceFiles(),
          environment.configuration,
        )
        .isIrBackend(true)
        .build()

    val compilerConfiguration = environment.configuration

    // Lower the IR tree before to convert it to a j2cl ast
    val lowerings = LoweringPasses(state, compilerConfiguration)
    IrGenerationExtension.registerExtension(environment.project, lowerings)

    // Register our CompilationUnitBuilder as an extension. It will be called once the IR tree is
    // built.
    val compilationUnitBuilderExtension =
      object : IrGenerationExtension {
        lateinit var compilationUnits: List<CompilationUnit>

        override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
          compilationUnits =
            CompilationUnitBuilder(
                KotlinEnvironment(
                  pluginContext,
                  packageAnnotationsResolver,
                  lowerings.jvmBackendContext,
                ),
                IntrinsicMethods(pluginContext.irBuiltIns),
              )
              .convert(moduleFragment)
        }
      }
    IrGenerationExtension.registerExtension(environment.project, compilationUnitBuilderExtension)

    JvmIrCodegenFactory(
        compilerConfiguration,
        compilerConfiguration.get(CLIConfigurationKeys.PHASE_CONFIG),
      )
      .convertToIr(
        CodegenFactory.IrConversionInput.fromGenerationStateAndFiles(
          state,
          environment.getSourceFiles(),
        )
      )

    return compilationUnitBuilderExtension.compilationUnits
  }

  private fun createCompilerConfiguration(sources: List<FileInfo>): CompilerConfiguration {
    val arguments = createCompilerArguments(sources)
    val configuration = CompilerConfiguration()

    val messageCollector = ProblemsMessageCollector(problems)
    configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
    configuration.put(CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY, messageCollector)
    configuration.put(IrMessageLogger.IR_MESSAGE_LOGGER, IrMessageCollector(messageCollector))

    // Used by Koltinc as name of the .kotlin_module file. This file will not be generated in our
    // case but this config item cannot be null. Currently only stdlib is passing a module name,
    // for others libraries we use the koltinc default name `main`
    configuration.put(
      CommonConfigurationKeys.MODULE_NAME,
      arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME,
    )

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

  private fun createCompilerArguments(sources: List<FileInfo>) =
    K2JVMCompilerArguments().also { arguments ->
      parseCommandLineArguments(kotlincopts, arguments)
      arguments.classpath = classpathEntries.joinToString(File.pathSeparator)
      arguments.commonSources =
        sources
          .filter { it.originalPath().startsWith("common-srcs/") }
          .map(FileInfo::sourcePath)
          .toTypedArray()
      arguments.freeArgs = sources.map(FileInfo::sourcePath)
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
