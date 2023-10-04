/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;

// TODO(b/303316598): This approach needs more thought for modular. With modular transpilation, we
// don't have enough information about JsEnum to propagate, in particular, the AST which includes
// the constant field values.
/** Propagates JsEnum constant fields. */
public class PropagateJsEnumConstants extends PropagateConstants {

  @Override
  protected boolean shouldPropagateConstant(FieldDescriptor fieldDescriptor) {
    return fieldDescriptor.isEnumConstant()
        && AstUtils.isNonNativeJsEnum(fieldDescriptor.getEnclosingTypeDescriptor());
  }

  // TODO(b/303309109) This is a workaround for NormalizeJsEnums not updating field references to
  // point to the correct descriptor. Ideally, this shouldn't be needed. Clean up after rethinking
  // JsEnum normalization passes.
  @Override
  protected FieldDescriptor getConstantFieldDescriptor(FieldAccess fieldAccess) {
    return AstUtils.createJsEnumConstantFieldDescriptor(fieldAccess.getTarget());
  }
}
