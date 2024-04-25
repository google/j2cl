/*
 * Copyright 2015 Google Inc.
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

import com.google.common.collect.ImmutableMap;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.ThisOrSuperReference;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeDescriptors.BootstrapType;

/**
 * Devirtualizes method calls to instance methods of Object, unboxed types (Boolean, Double, String)
 * and their super classes and super interfaces to corresponding static method calls.
 */
public class DevirtualizeMethodCalls extends NormalizationPass {

  /**
   * Mapping from the TypeDeclaration, whose instance methods should be devirtualized, to the
   * TypeDescriptor that contains the devirtualized static methods that should be dispatched to.
   *
   * <p>The instance methods of unboxed types (Boolean, Double, String) are translated to
   * corresponding static methods that are translated automatically by J2cl. Instance methods of
   * Object and the super classes/interfaces of unboxed types are translated to the trampoline
   * methods which are implemented in corresponding types (Objects, Numbers, etc.).
   */
  private final ImmutableMap<TypeDeclaration, DeclaredTypeDescriptor>
      devirtualizeTargetByOriginType =
          ImmutableMap.<TypeDeclaration, DeclaredTypeDescriptor>builder()
              .put(
                  TypeDescriptors.get().javaLangObject.getTypeDeclaration(),
                  BootstrapType.OBJECTS.getDescriptor())
              .put(
                  TypeDescriptors.get().javaLangBoolean.getTypeDeclaration(),
                  TypeDescriptors.get().javaLangBoolean)
              .put(
                  TypeDescriptors.get().javaLangDouble.getTypeDeclaration(),
                  TypeDescriptors.get().javaLangDouble)
              .put(
                  TypeDescriptors.get().javaLangString.getTypeDeclaration(),
                  TypeDescriptors.get().javaLangString)
              .put(
                  TypeDescriptors.get().javaLangNumber.getTypeDeclaration(),
                  BootstrapType.NUMBERS.getDescriptor())
              .put(
                  TypeDescriptors.get().javaLangComparable.getTypeDeclaration(),
                  BootstrapType.COMPARABLES.getDescriptor())
              .put(
                  TypeDescriptors.get().javaLangCharSequence.getTypeDeclaration(),
                  BootstrapType.CHAR_SEQUENCES.getDescriptor())
              .build();

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor targetMethodDescriptor = methodCall.getTarget();

            if (!targetMethodDescriptor.isInstanceMember()) {
              return methodCall;
            }

            if (targetMethodDescriptor.isJsFunction()) {
              return methodCall;
            }

            DeclaredTypeDescriptor originType = targetMethodDescriptor.getEnclosingTypeDescriptor();
            if (originType.isJsFunctionImplementation()) {
              return AstUtils.devirtualizeMethodCall(methodCall, originType);
            }

            // Do *not* perform Object method devirtualization. The point with super method calls
            // is to *not* call the default version of the method on the prototype and instead call
            // the specific version of the method in the super class. If we were to perform Object
            // method devirtualization then the resulting routing through Objects.doFoo() would end
            // up calling back onto the version of the method on the prototype (aka the wrong one).
            // Also as an optimization we do not perform devirtualization on 'this' method calls as
            // the trampoline is not necessary.
            if (methodCall.getQualifier() instanceof ThisOrSuperReference
                || methodCall.isStaticDispatch()) {
              return methodCall;
            }

            DeclaredTypeDescriptor targetType =
                devirtualizeTargetByOriginType.get(originType.getTypeDeclaration());

            if (targetType == null) {
              return methodCall;
            }

            // If this is a trampoline call and the dispatch is done from trampoline itself, then we
            // shouldn't devirtualize the calls.
            // TODO(b/240721785): stop using getQualifiedJsName for comparison when BootStrapTypes
            // are no longer synthetic.
            boolean isTrampoline = !targetType.isSameBaseType(originType);
            if (isTrampoline
                && getCurrentType().getQualifiedJsName().equals(targetType.getQualifiedJsName())) {
              return methodCall;
            }

            return AstUtils.devirtualizeMethodCall(methodCall, targetType);
          }
        });
  }
}
