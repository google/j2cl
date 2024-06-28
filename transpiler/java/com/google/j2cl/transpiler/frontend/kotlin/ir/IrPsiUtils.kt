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
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin.ir

import com.google.j2cl.common.FilePosition
import com.google.j2cl.common.SourcePosition
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.PsiSourceManager.findPsiElement
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

fun IrElement.getNamedPsiElement(irFile: IrFile): PsiElement? {
  val psiElement = getPsiElement(irFile) ?: return null
  return when {
    psiElement is KtConstructor<*> -> psiElement.getConstructorKeyword() ?: psiElement
    psiElement is PsiNameIdentifierOwner -> psiElement.nameIdentifier ?: psiElement
    else -> psiElement
  }
}

fun IrElement.getPsiElement(irFile: IrFile): PsiElement? {
  var psiElement =
    when {
      isTemporaryVariable -> null
      isPrefixExpression -> findPsiElement(this, irFile, KtPrefixExpression::class)
      isPostfixExpression -> findPsiElement(this, irFile, KtPostfixExpression::class)
      isAugmentedAssignement || isFieldAssignment ->
        findPsiElement(this, irFile, KtBinaryExpression::class)
      isVariableDeclaration -> findPsiElement(this, irFile, KtVariableDeclaration::class)
      isFieldDeclaration -> findPsiElement(this, irFile, KtProperty::class)
      isLabeledExpression -> findPsiElement(this, irFile, KtLabeledExpression::class)
      // Map primary constructor defined without the constructor keyword to the class name.
      isPrimaryConstructor && findPsiElement(this, irFile) is KtParameterList ->
        findPsiElement(this, irFile, KtClass::class)
      else -> findPsiElement(this, irFile)
    } ?: return null

  return when {
    // In the mapping, KtClass elements that don't correspond to enum entries are represented by
    // their class name element. So the mapping contains only the class name and not the full
    // class code.
    psiElement is KtClass && psiElement !is KtEnumEntry -> psiElement.nameIdentifier
    psiElement is KtObjectDeclaration -> psiElement.nameIdentifier ?: psiElement.getObjectKeyword()
    else -> psiElement
  }
}

fun PsiElement.getSourcePosition(irFile: IrFile, name: String? = null): SourcePosition {
  return irFile.fileEntry
    .getSourceRangeInfo(this.startOffset, this.endOffset)
    .toSourcePosition(name)
}

private fun SourceRangeInfo.toSourcePosition(name: String? = null): SourcePosition {
  // The node is not part of the original code source.
  if (startOffset < 0 || endOffset < 0 || filePath.isEmpty()) {
    return SourcePosition.NONE
  }

  return SourcePosition.newBuilder()
    .setFilePath(filePath)
    .setName(name)
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
