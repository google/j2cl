/*
 * Copyright 2024 Google Inc.
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

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile

internal typealias J2clLoweringPassFactory = (J2clBackendContext) -> FileLoweringPass

internal typealias JvmLoweringPassFactory = (JvmBackendContext) -> FileLoweringPass

internal fun JvmLoweringPassFactory.toJ2clLoweringPassFactory(): J2clLoweringPassFactory =
  { ctx: J2clBackendContext ->
    invoke(ctx.jvmBackendContext)
  }

internal fun (() -> FileLoweringPass).toJ2clLoweringPassFactory(): J2clLoweringPassFactory =
  { _: J2clBackendContext ->
    invoke()
  }

internal fun ((JvmBackendContext) -> BodyLoweringPass).asPostfix(): JvmLoweringPassFactory =
  { context: JvmBackendContext ->
    invoke(context).asPostfix()
  }

private fun BodyLoweringPass.asPostfix(): FileLoweringPass =
  object : FileLoweringPass {
    override fun lower(irFile: IrFile) {
      this@asPostfix.runOnFilePostfix(irFile, withLocalDeclarations = true)
    }
  }
