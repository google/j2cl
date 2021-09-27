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
package com.google.j2cl.transpiler.backend.kotlin

import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptor

/**
 * Emits the proper fully rendered type for the give type descriptor at the current location.
 * TODO(dpo): Move this to a better long term place (this logic is likely to get pretty complex).
 */
fun Renderer.renderTypeDescriptor(typeDescriptor: TypeDescriptor) {
  when (typeDescriptor) {
    is ArrayTypeDescriptor -> renderArrayTypeDescriptor(typeDescriptor)
    // TODO(dpo): Other type descriptor logic.
    else -> renderTypeDescriptorDescription(typeDescriptor)
  }
}

private fun Renderer.renderArrayTypeDescriptor(arrayTypeDescriptor: ArrayTypeDescriptor) {
  render("Array")
  renderInAngleBrackets { renderTypeDescriptor(arrayTypeDescriptor.componentTypeDescriptor!!) }
}

private fun Renderer.renderTypeDescriptorDescription(typeDescriptor: TypeDescriptor) {
  render(typeDescriptor.readableDescription)
}
