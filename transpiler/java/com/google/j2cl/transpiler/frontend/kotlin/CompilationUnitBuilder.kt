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

import com.google.j2cl.common.FilePosition
import com.google.j2cl.common.SourcePosition
import com.google.j2cl.transpiler.ast.CompilationUnit
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.Visibility
import com.google.j2cl.transpiler.frontend.common.AbstractCompilationUnitBuilder
import java.util.ArrayList
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.isClass

internal fun Fir2IrResult.getCompilationUnits(): List<CompilationUnit> =
  CompilationUnitBuilder(KotlinEnvironment(components)).convert(irModuleFragment)

internal fun IrDeclarationWithVisibility.getJ2clVisibility(): Visibility =
  convertVisibility(this.visibility.delegate)

private fun convertVisibility(visibility: org.jetbrains.kotlin.descriptors.Visibility): Visibility =
  when (visibility) {
    // Internal means that the owner is visible inside the kotlin module. When you call internal
    // kotlin members from java, they are considered as public.
    Visibilities.Public,
    Visibilities.Internal -> Visibility.PUBLIC
    Visibilities.Protected -> Visibility.PROTECTED
    else -> Visibility.PRIVATE
  }

/** Creates a J2CL Java AST from Kotlin IR. */
class CompilationUnitBuilder(private val environment: KotlinEnvironment) :
  AbstractCompilationUnitBuilder() {
  private val compilationUnits: MutableList<CompilationUnit> = ArrayList()
  private lateinit var currentIrFile: IrFile

  fun convert(irModuleFragment: IrModuleFragment): List<CompilationUnit> {
    irModuleFragment.files.forEach(::convertFile)
    return compilationUnits
  }

  private fun convertFile(irFile: IrFile) {
    currentIrFile = irFile
    currentCompilationUnit = CompilationUnit(irFile.fileEntry.name, irFile.fqName.asString())
    compilationUnits.add(currentCompilationUnit)

    irFile.declarations.forEach(::convertDeclaration)
  }

  private fun convertDeclaration(irDeclaration: IrDeclaration) {
    when (irDeclaration) {
      is IrClass -> convertClass(irDeclaration)
      else -> throw RuntimeException("Declaration not yet supported: $irDeclaration")
    }
  }

  private fun convertClass(irClass: IrClass) {
    require(irClass.isClass) { "Element not yet supported: $irClass" }

    val type =
      Type(
        getSourcePosition(irClass),
        irClass.getJ2clVisibility(),
        environment.getDeclarationForType(irClass)
      )
    currentCompilationUnit.addType(type)
  }

  private fun getSourcePosition(element: IrElement): SourcePosition =
    currentIrFile
      .fileEntry
      .getSourceRangeInfo(element.startOffset, element.endOffset)
      .toSourcePosition()
}

private fun SourceRangeInfo.toSourcePosition(): SourcePosition {
  // The node is not part of the original code source.
  if (startOffset < 0 || endOffset < 0) {
    return SourcePosition.NONE
  }

  return SourcePosition.newBuilder()
    .setFilePath(filePath)
    .setStartFilePosition(
      FilePosition.newBuilder()
        .setLine(startLineNumber)
        .setColumn(startColumnNumber)
        .setByteOffset(startOffset)
        .build()
    )
    .setEndFilePosition(
      FilePosition.newBuilder()
        .setLine(endLineNumber)
        .setColumn(endColumnNumber)
        .setByteOffset(endOffset)
        .build()
    )
    .build()
}
