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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.j2cl.transpiler.ast.AstUtils.isNonNativeJsEnum;

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.Node;

/**
 * Creates an Object[] literal instead of a JsEnum[] literal for T varargs where T is specialized to
 * a JsEnum.
 *
 * <p>This is to allow parameterizing a varargs methods to a JsEnum[], which needs to work around
 * the non assignability of JsEnum[] to Object[]>
 */
public final class NormalizePackagedJsEnumVarargsLiterals extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteInvocation(Invocation invocation) {
            if (!invocation.getTarget().isVarargs()) {
              return invocation;
            }
            return maybeNormalizePackagedJsEnumVarargsLiteral(invocation);
          }
        });
  }

  /**
   * For method calls with varargs uses the erased type of the original type parameter as the array
   * component of the vararg if it's specialized to a JsEnum type.
   *
   * <p>Calls with varargs should have already be packed into an array literal before this is
   * called.
   *
   * <p>For example: {@code foo(nonVararg, new SomeJsEnum[] {SomeJsEnum.A, SomeJsEnum.B}} would be
   * rewritten to {@code foo(nonVararg, new Object[] {SomeJsEnum.A, SomeJsEnum.B}}.
   */
  public static Invocation maybeNormalizePackagedJsEnumVarargsLiteral(Invocation invocation) {
    if (!invocation.getTarget().isVarargs()) {
      return invocation;
    }

    checkArgument(
        invocation.getTarget().getParameterDescriptors().size() == invocation.getArguments().size(),
        "varargs should have been packaged into an array literal before running this pass.");

    Expression lastArgument = Iterables.getLast(invocation.getArguments());
    var varargsTypeDescriptor =
        (ArrayTypeDescriptor)
            Iterables.getLast(invocation.getTarget().getParameterDescriptors()).getTypeDescriptor();

    if (!isNonNativeJsEnum(varargsTypeDescriptor.getComponentTypeDescriptor())
        || !(lastArgument instanceof ArrayLiteral)) {
      return invocation;
    }

    // TODO(b/118615488): remove this when BoxedLightEnums are surfaces to J2CL.
    //
    // Here we create an array using the bound of declared type T[] instead of the actual inferred
    // type JsEnum[] since non-native JsEnum arrays are forbidden.
    // We have chosen this workaround instead of banning T[] when T is inferred to be a non-native
    // JsEnum. It is quite uncommon to have code that observes the implications of using an array of
    // the supertype in the implicit array creation due to varargs, instead of an array of the
    // inferred subtype. Making this choice allows the use of common varargs APIs such as
    // Arrays.asList() with JsEnum values.

    var varargsParameterDeclaration =
        Iterables.getLast(
            invocation.getTarget().getDeclarationDescriptor().getParameterDescriptors());

    return Invocation.Builder.from(invocation)
        .replaceVarargsArgument(
            new ArrayLiteral(
                (ArrayTypeDescriptor)
                    varargsParameterDeclaration.getTypeDescriptor().toRawTypeDescriptor(),
                ((ArrayLiteral) lastArgument).getValueExpressions()))
        .build();
  }
}
