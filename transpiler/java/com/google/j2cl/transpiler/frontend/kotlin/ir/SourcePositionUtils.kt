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
package com.google.j2cl.transpiler.frontend.kotlin.ir

import com.google.j2cl.common.FilePosition
import com.google.j2cl.common.SourcePosition
import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.KtNodeTypes.BACKING_FIELD
import org.jetbrains.kotlin.KtNodeTypes.CLASS
import org.jetbrains.kotlin.KtNodeTypes.ENUM_ENTRY
import org.jetbrains.kotlin.KtNodeTypes.FUN
import org.jetbrains.kotlin.KtNodeTypes.OBJECT_DECLARATION
import org.jetbrains.kotlin.KtNodeTypes.PRIMARY_CONSTRUCTOR
import org.jetbrains.kotlin.KtNodeTypes.PROPERTY
import org.jetbrains.kotlin.KtNodeTypes.SECONDARY_CONSTRUCTOR
import org.jetbrains.kotlin.KtNodeTypes.VALUE_PARAMETER
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.backend.jvm.MultifileFacadeFileEntry
import org.jetbrains.kotlin.diagnostics.findChildByType
import org.jetbrains.kotlin.diagnostics.findDescendantByType
import org.jetbrains.kotlin.diagnostics.nameIdentifier
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.lexer.KtTokens.COMPANION_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.CONSTRUCTOR_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.OBJECT_KEYWORD

fun IrElement.getSourcePosition(irFile: IrFile): SourcePosition =
  sourcePositionNoneIfMultifileFacadeFile(irFile) ?: getSourcePositionFromIr(irFile)

private fun IrElement.getSourcePositionFromIr(
  irFile: IrFile,
  name: String? = null,
): SourcePosition {
  if (isTemporaryVariable) {
    // Do not map synthetic variable, they do belong to the source.
    return SourcePosition.NONE
  }

  // The source position of an IrReturn without `return` keyword is incorrect. Take the source
  // position of the returned expression.
  if (this is IrReturn && startOffset == endOffset) {
    return value.getSourcePositionFromIr(irFile, name)
  }

  return irFile.getSourcePosition(startOffset, endOffset, name)
}

/**
 * Returns the source position of the symbol for this element. If the original symbol name will be
 * changed or removed in the generated JavaScript output, the original name can be preserved by
 * providing it in the `originalSymbolName` parameter.
 */
fun IrElement.getNameSourcePosition(irFile: IrFile, originalSymbolName: String?) =
  sourcePositionNoneIfMultifileFacadeFile(irFile)
    ?:
    // Try to find an accurate position if source information is available
    getNameSourcePositionFromSource(irFile, originalSymbolName)
    // otherwise return the position from the IR node.
    ?: getSourcePositionFromIr(irFile, originalSymbolName)

private fun IrElement.getNameSourcePositionFromSource(irFile: IrFile, name: String?) =
  getNameSourceElementForSourceMap()?.getSourcePosition(irFile, name)

private fun IrElement.getNameSourceElementForSourceMap(): LighterASTNode? {
  val sourceElement = getSourceElement() ?: return null

  val astNode = sourceElement.lighterASTNode

  return with(sourceElement.treeStructure) {
    when (sourceElement.elementType) {
      BACKING_FIELD,
      CLASS,
      FUN,
      VALUE_PARAMETER,
      PROPERTY,
      ENUM_ENTRY -> nameIdentifier(astNode)
      PRIMARY_CONSTRUCTOR,
      SECONDARY_CONSTRUCTOR ->
        // Return the `constructor` keyword. It may be absent for primary ctor, in that case we
        // return the class name identifier.
        findChildByType(astNode, CONSTRUCTOR_KEYWORD)
          ?: nameIdentifier(checkNotNull(getParent(astNode)?.takeIf { it.tokenType == CLASS }))
      OBJECT_DECLARATION ->
        // For object declaration, we return the name of the object if it exists, otherwise we look
        // for the `companion` keyword element for companion object, otherwise we look for the
        // `object` keyword.
        nameIdentifier(astNode)
          ?: findDescendantByType(astNode, COMPANION_KEYWORD)
          ?: findDescendantByType(astNode, OBJECT_KEYWORD)
      else -> astNode
    }
  }
}

private fun IrElement.getSourceElement() =
  when (this) {
    is IrEnumEntry -> getSourceElement()
    else -> firElement?.source
  }

/** Retrieves the sourceElement of the enum entry through the parent enum class. */
private fun IrEnumEntry.getSourceElement(): KtSourceElement? {
  val firEnclosingClass = parent.firElement as? FirRegularClass ?: return null
  @OptIn(DirectDeclarationsAccess::class)
  return firEnclosingClass.declarations
    .filterIsInstance<FirEnumEntry>()
    .single { it.name == this.name }
    .source
}

private val IrElement.firElement: FirDeclaration?
  get() = ((this as? IrMetadataSourceOwner)?.metadata as? FirMetadataSource)?.fir

private fun LighterASTNode.getSourcePosition(irFile: IrFile, name: String?): SourcePosition =
  irFile.getSourcePosition(this.startOffset, this.endOffset, name)

private fun IrFile.getSourcePosition(
  startOffset: Int,
  endOffset: Int,
  name: String?,
): SourcePosition {
  // The node is not part of the original code source.
  if (startOffset < 0 || endOffset < 0 || fileEntry.name.isEmpty()) {
    return SourcePosition.NONE
  }

  val sourceRange = fileEntry.getSourceRangeInfo(startOffset, endOffset)

  return SourcePosition.newBuilder()
    .setFilePath(sourceRange.filePath)
    .setName(name)
    .setStartFilePosition(
      FilePosition.newBuilder()
        .setLine(sourceRange.startLineNumber)
        .setColumn(sourceRange.startColumnNumber)
        .setByteOffset(sourceRange.startOffset)
        .build()
    )
    .setEndFilePosition(
      FilePosition.newBuilder()
        .setLine(sourceRange.endLineNumber)
        .setColumn(sourceRange.endColumnNumber)
        .setByteOffset(sourceRange.endOffset)
        .build()
    )
    .build()
}

// TODO(b/324630289):add support for multifilefacade file.
private fun sourcePositionNoneIfMultifileFacadeFile(irFile: IrFile): SourcePosition? =
  if (irFile.fileEntry is MultifileFacadeFileEntry) SourcePosition.NONE else null
