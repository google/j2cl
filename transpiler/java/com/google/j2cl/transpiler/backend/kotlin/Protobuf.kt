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

internal fun isProtobufGetter(methodDescriptor: MethodDescriptor): Boolean {
  if (
    methodDescriptor.isStatic() ||
      !methodDescriptor.getParameterDescriptors().isEmpty() ||
      !isProtobufMessageOrBuilder(methodDescriptor.getEnclosingTypeDescriptor())
  ) {
    return false
  }
  val name = methodDescriptor.name ?: ""
  return name.startsWith("get") && name != "getDefaultInstance"
}

private fun isProtobufMessageOrBuilder(typeDescriptor: DeclaredTypeDescriptor): Boolean {
  val superTypeDescriptor: DeclaredTypeDescriptor =
    typeDescriptor.getSuperTypeDescriptor() ?: return false
  val name = superTypeDescriptor.qualifiedSourceName
  return name == "com.google.protobuf.GeneratedMessage" ||
    name == "com.google.protobuf.GeneratedMessageLite" ||
    name == "com.google.protobuf.GeneratedMessage.Builder" ||
    name == "com.google.protobuf.GeneratedMessageLite.Builder"
}
