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
package com.google.j2cl.ast.visitors;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;

/** Make the implicit parameters and super calls in enum constructors explicit. */
public class MakeEnumConstructionsExplicit extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new Rewriter());
  }

  private static class Rewriter extends AbstractRewriter {
    private static final String ORDINAL_PARAMETER_NAME = "$ordinal";
    private static final String VALUE_NAME_PARAMETER_NAME = "$name";
    private final Multiset<TypeDescriptor> ordinalsByEnumTypeDescriptor = HashMultiset.create();
    private final Variable ordinalVariable =
        new Variable(ORDINAL_PARAMETER_NAME, TypeDescriptors.get().primitiveInt, false, true);
    private final Variable nameVariable =
        new Variable(VALUE_NAME_PARAMETER_NAME, TypeDescriptors.get().javaLangString, false, true);

    @Override
    public Node rewriteMethod(Method method) {
      /*
       * Only add parameters to constructor methods in Enum classes..
       */
      if (!method.isConstructor() || !getCurrentType().getDescriptor().isEnumOrSubclass()) {
        return method;
      }
      return Method.Builder.from(method).addParameters(nameVariable, ordinalVariable).build();
    }

    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      /*
       * Only add arguments to super() calls inside of constructor methods in Enum classes.
       */
      if (!getCurrentMember().isConstructor()
          || !getCurrentType().getDescriptor().isEnumOrSubclass()
          || !methodCall.getTarget().isConstructor()) {
        return methodCall;
      }

      return MethodCall.Builder.from(methodCall)
          .appendArgumentsAndUpdateDescriptor(
              nameVariable.getReference(), ordinalVariable.getReference())
          .build();
    }

    @Override
    public Node rewriteNewInstance(NewInstance newInstance) {
      // Rewrite newInstances for the creation of the enum constants to include the assigned ordinal
      // and name.
      if (!getCurrentType().isEnum()
          || !getCurrentMember().isField()
          || !((Field) getCurrentMember())
              .getDescriptor()
              .getTypeDescriptor()
              .equalsIgnoreNullability(getCurrentType().getDescriptor())) {

        // Enum constants creations are exactly those that are field initializers for fields
        // whose class is then enum class.
        return newInstance;
      }
      // This is definitely an enum initialization NewInstance.

      Field enumField = (Field) getCurrentMember();
      Preconditions.checkState(
          enumField != null,
          "Enum values can only be instantiated inside their field initialization");

      int currentOrdinal =
          ordinalsByEnumTypeDescriptor.add(
              enumField.getDescriptor().getEnclosingClassTypeDescriptor(), 1);

      return NewInstance.Builder.from(newInstance)
          .appendArgumentsAndUpdateDescriptor(
              StringLiteral.fromPlainText(enumField.getDescriptor().getName()),
              new NumberLiteral(TypeDescriptors.get().primitiveInt, currentOrdinal))
          .build();
    }
  }
}
