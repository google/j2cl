/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.JvmInventNamesForLocalClassesImpl

// Class definition copied from org.jetbrains.kotlin.backend.jvm.lower.JvmInventNamesForLocalClasses
// to change the visibility.
internal class JvmInventNamesForLocalClasses(context: JvmBackendContext) :
  JvmInventNamesForLocalClassesImpl(context, false)
