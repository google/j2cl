/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.frontend.kotlin.klib

import com.google.j2cl.transpiler.frontend.kotlin.jklib.K2JKlibCompiler
import com.google.j2cl.transpiler.frontend.kotlin.jklib.K2JKlibCompilerArguments
import com.intellij.openapi.util.Disposer
import java.io.File
import kotlin.system.exitProcess
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.setupCommonArguments
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.util.PerformanceManager
import org.kohsuke.args4j.Argument
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option

/**
 * Compiler to generate header klibs from Kotlin sources.
 *
 * This is a simple wrapper around the [K2JKlibCompiler].
 */
public class HeaderKlibCompiler() {
  @Argument(
    metaVar = "<source files>",
    required = true,
    usage = "Specifies individual files and jars/zips of sources.",
  )
  lateinit var sources: List<String>

  @Option(name = "-kotlincOptions", usage = "Flags passed to the Kotlin compiler.", required = true)
  var kotlincOptions: MutableList<String> = ArrayList()

  internal fun compile() {
    check(sources.none { it.endsWith(".java") }) { "Java input files are not supported." }

    val arguments =
      K2JKlibCompilerArguments().apply {
        parseCommandLineArguments(kotlincOptions, this)
        freeArgs = sources
      }

    val configuration =
      CompilerConfiguration().apply {
        val messageCollector =
          object : MessageCollector {
            var hasErrors = false

            override fun clear() {}

            override fun report(
              severity: CompilerMessageSeverity,
              message: String,
              location: CompilerMessageSourceLocation?,
            ) {
              if (!severity.isError) {
                return
              }
              hasErrors = true
              System.err.println(
                "${severity.name}\t${location?.path ?: ""}:${location?.line ?: ""} \t$message"
              )
            }

            override fun hasErrors() = hasErrors
          }

        put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        put(CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY, messageCollector)
        put(
          CLIConfigurationKeys.PERF_MANAGER,
          object : PerformanceManager(JvmPlatforms.defaultJvmPlatform, "HeaderKlibGenerator") {},
        )
        setupCommonArguments(arguments) { versionArray -> MetadataVersion(*versionArray) }
      }

    val disposable = Disposer.newDisposable("HeaderKlibCompiler Disposable")

    val exitCode =
      try {
        K2JKlibCompiler().doExecute(arguments, configuration, disposable, null)
      } finally {
        Disposer.dispose(disposable)
      }

    exitProcess(exitCode.code)
  }
}

fun main(args: Array<String>) {
  HeaderKlibCompiler().apply { CmdLineParser(this).parseArgument(expandFlagFile(args)) }.compile()
}

/**
 * Loads a potential flag file and returns the flags. Flag files are only allowed as the last
 * parameter and need to start with an '@'.
 */
private fun expandFlagFile(args: Array<String>): List<String> {
  val combinedArgs = args.toMutableList()
  combinedArgs
    .lastOrNull()
    ?.takeIf { it.startsWith("@") }
    ?.let { flagFileArg ->
      combinedArgs.removeLast()
      combinedArgs.addAll(File(flagFileArg.substring(1)).readLines().filter { it.isNotEmpty() })
    }
  return combinedArgs
}
