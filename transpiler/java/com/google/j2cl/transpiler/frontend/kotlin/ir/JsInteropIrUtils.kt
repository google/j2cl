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
// TODO(b/550323040): update this file to use the new IrAnnotation node.
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin.ir

import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_OPTIONAL_ANNOTATION_NAME
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.name.FqName

val IrValueParameter.isJsOptional: Boolean
  get() = getAnnotation(JS_OPTIONAL_ANNOTATION_FQ_NAME) != null

private val JS_OPTIONAL_ANNOTATION_FQ_NAME: FqName = FqName(JS_OPTIONAL_ANNOTATION_NAME)
