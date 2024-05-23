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
package com.google.j2cl.transpiler.backend.kotlin

import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.backend.kotlin.common.camelCaseStartsWith

internal fun MethodDescriptor.isProtoExtensionChecker() =
  qualifiedBinaryName ==
    "com.google.protobuf.GeneratedMessageLite\$ExtendableMessage.hasExtension" ||
    qualifiedBinaryName == "com.google.protobuf.GeneratedMessage\$ExtendableMessage.hasExtension"

internal fun MethodDescriptor.isProtoExtensionGetter() =
  qualifiedBinaryName ==
    "com.google.protobuf.GeneratedMessageLite\$ExtendableMessage.getExtension" ||
    qualifiedBinaryName == "com.google.protobuf.GeneratedMessage\$ExtendableMessage.getExtension"

internal fun MethodDescriptor.isProtobufGetter(): Boolean {
  if (!isProtobufMethod() || !parameterDescriptors.isEmpty()) {
    return false
  }
  val name = name ?: ""
  return (name.startsWith("get") && name != "getDefaultInstance")
}

internal fun MethodDescriptor.isProtobufMethod() =
  !isStatic && enclosingTypeDescriptor.isProtobufMessageOrBuilder()

internal fun TypeDescriptor.isProtobufBuilder(): Boolean {
  if (this !is DeclaredTypeDescriptor) {
    return false
  }
  val superTypeDescriptor: DeclaredTypeDescriptor = superTypeDescriptor ?: return false
  val name = superTypeDescriptor.qualifiedSourceName
  return name == "com.google.protobuf.GeneratedMessage.Builder" ||
    name == "com.google.protobuf.GeneratedMessageLite.Builder" ||
    name == "com.google.protobuf.GeneratedMessageLite.ExtendableBuilder"
}

internal fun computeProtobufPropertyName(methodName: String) =
  if (methodName.camelCaseStartsWith("get")) {
    methodName[3].toLowerCase() + methodName.substring(4)
  } else {
    methodName
  }

private fun DeclaredTypeDescriptor.isProtobufMessage(): Boolean {
  val superTypeDescriptor: DeclaredTypeDescriptor = superTypeDescriptor ?: return false
  val name = superTypeDescriptor.qualifiedSourceName
  return name == "com.google.protobuf.GeneratedMessage" ||
    name == "com.google.protobuf.GeneratedMessageLite" ||
    name == "com.google.protobuf.GeneratedMessageLite.ExtendableMessage"
}

internal fun DeclaredTypeDescriptor.isProtobufMessageOrBuilder() =
  isProtobufMessage() || isProtobufBuilder()
