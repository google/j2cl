/*
 * Copyright 2022 Google Inc.
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

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.VisibilityPolicy
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.NameUtils

/**
 * A pass that encapsulates org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering to
 * only moves local functions up to the closest enclosing type and rewrite local function calls
 * accordingly. The original pass moves all local declarations including local classes.
 */
internal class LocalFunctionLowering(private val context: JvmBackendContext) : BodyLoweringPass {

  val localDeclarationLowering =
    LocalDeclarationsLowering(context, NameUtils::sanitizeAsJavaIdentifier, visibilityPolicy)

  override fun lower(irBody: IrBody, container: IrDeclaration) {
    localDeclarationLowering.lower(irBody, container, classesToLower = emptySet<IrClass>())
  }
}

val visibilityPolicy =
  object : VisibilityPolicy {
    override fun forCapturedField(value: IrValueSymbol): DescriptorVisibility =
      JavaDescriptorVisibilities.PACKAGE_VISIBILITY // avoid requiring a synthetic accessor for it
  }
