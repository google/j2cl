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

import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;

/**
 * Removes {@code $isInstance} support methods. {@code $isInstance} is unused in Wasm, and the
 * explicit implementations in our JRE in {@code Comparable}, etc use a native type in a way that is
 * unsupported in Wasm.
 */
public class RemoveIsInstanceMethods extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    type.getMembers().removeIf(RemoveIsInstanceMethods::isIsInstanceMethod);
  }

  private static boolean isIsInstanceMethod(Member member) {
    return member.isMethod()
        && member.getDescriptor().getName().equals(MethodDescriptor.IS_INSTANCE_METHOD_NAME);
  }
}
