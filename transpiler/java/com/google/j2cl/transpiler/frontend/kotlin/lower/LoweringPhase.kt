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
package com.google.j2cl.transpiler.frontend.kotlin.lower

import com.google.common.collect.ImmutableList
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.wrapWithCompilationException
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus

internal class LoweringPhase
private constructor(
  private val loweringPassFactories: ImmutableList<(J2clBackendContext) -> ModuleLoweringPass>
) {
  fun lower(context: J2clBackendContext, module: IrModuleFragment) {
    for (factory in loweringPassFactories) {
      ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
      factory(context).lower(module)
    }
  }

  internal class Builder {
    private val loweringPassFactories = mutableListOf<(J2clBackendContext) -> ModuleLoweringPass>()

    @JvmName("moduleLoweringJ2cl")
    fun moduleLowering(factory: (J2clBackendContext) -> ModuleLoweringPass) {
      loweringPassFactories.add(factory)
    }

    @JvmName("moduleLoweringJvm")
    fun moduleLowering(factory: (JvmBackendContext) -> ModuleLoweringPass) {
      loweringPassFactories.add(factory.withJ2clContext())
    }

    @JvmName("moduleLoweringCommon")
    fun moduleLowering(factory: (CommonBackendContext) -> ModuleLoweringPass) {
      loweringPassFactories.add(factory)
    }

    @JvmName("perFileLoweringJ2cl")
    fun perFileLowering(factory: (J2clBackendContext) -> FileLoweringPass) {
      loweringPassFactories.add(factory.asPerFileLowering())
    }

    @JvmName("perFileLoweringJvm")
    fun perFileLowering(factory: (JvmBackendContext) -> FileLoweringPass) {
      loweringPassFactories.add(factory.asPerFileLowering().withJ2clContext())
    }

    @JvmName("perFileLoweringCommon")
    fun perFileLowering(factory: (CommonBackendContext) -> FileLoweringPass) {
      loweringPassFactories.add(factory.asPerFileLowering())
    }

    fun perFileLowering(factory: () -> FileLoweringPass) {
      loweringPassFactories.add({ _: CommonBackendContext -> factory() }.asPerFileLowering())
    }

    fun build() = LoweringPhase(ImmutableList.copyOf(loweringPassFactories))
  }
}

internal fun loweringPhase(builder: LoweringPhase.Builder.() -> Unit): LoweringPhase =
  LoweringPhase.Builder().apply(builder).build()

private class PerFileLowering<T : CommonBackendContext>(
  private val context: T,
  private val fileLoweringFactory: (T) -> FileLoweringPass,
) : ModuleLoweringPass {
  override fun lower(irModule: IrModuleFragment) {
    for (f in irModule.files) {
      try {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        fileLoweringFactory(context).lower(f)
      } catch (e: CompilationException) {
        e.initializeFileDetails(f)
        throw e
      } catch (e: Throwable) {
        throw e.wrapWithCompilationException("Internal error in file lowering", f, null)
      }
    }
  }
}

private fun <T : CommonBackendContext> ((T) -> FileLoweringPass).asPerFileLowering() =
  { context: T ->
    PerFileLowering(context, this@asPerFileLowering)
  }

private fun ((JvmBackendContext) -> ModuleLoweringPass).withJ2clContext():
  (J2clBackendContext) -> ModuleLoweringPass = { context -> this.invoke(context.jvmBackendContext) }
