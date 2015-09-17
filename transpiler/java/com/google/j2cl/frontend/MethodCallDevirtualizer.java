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
package com.google.j2cl.frontend;

import com.google.j2cl.ast.ASTUtils;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

import org.eclipse.jdt.core.dom.IMethodBinding;

import java.util.Arrays;
import java.util.List;

/**
 * Performs method devirtualizations.
 */
public class MethodCallDevirtualizer {
  interface Devirtualizer {
    boolean shouldBeDevirtualized(
        MethodCall methodCall,
        IMethodBinding methodBinding,
        org.eclipse.jdt.core.dom.CompilationUnit compilationUnit);

    TypeDescriptor getEnclosingClassTypeDescriptor();

    TypeDescriptor getInstanceTypeDescriptor();
  }

  private static Devirtualizer objectDevirtualizer =
      new Devirtualizer() {
        @Override
        public boolean shouldBeDevirtualized(
            MethodCall methodCall,
            IMethodBinding methodBinding,
            org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
          // Do not devirtualize inside the same declaring class, because it does not need to go
          // through the trampoline path in Objects functions.
          return JdtUtils.isObjectInstanceMethodBinding(methodBinding, compilationUnit)
              && dispatchesToSomeOtherClass(methodCall);
        }

        @Override
        public TypeDescriptor getEnclosingClassTypeDescriptor() {
          return TypeDescriptors.OBJECTS_TYPE_DESCRIPTOR;
        }

        @Override
        public TypeDescriptor getInstanceTypeDescriptor() {
          return TypeDescriptors.OBJECT_TYPE_DESCRIPTOR;
        }
      };

  private static Devirtualizer numberDevirtualizer =
      new Devirtualizer() {
        @Override
        public boolean shouldBeDevirtualized(
            MethodCall methodCall,
            IMethodBinding methodBinding,
            org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
          // Do not devirtualize inside the same declaring class, because it does not need to go
          // through the trampoline path in Numbers functions.
          return JdtUtils.isNumberInstanceMethodBinding(methodBinding, compilationUnit)
              && dispatchesToSomeOtherClass(methodCall);
        }

        @Override
        public TypeDescriptor getEnclosingClassTypeDescriptor() {
          return TypeDescriptors.NUMBERS_TYPE_DESCRIPTOR;
        }

        @Override
        public TypeDescriptor getInstanceTypeDescriptor() {
          return TypeDescriptors.NUMBER_TYPE_DESCRIPTOR;
        }
      };

  private static Devirtualizer booleanDevirtualizer =
      new Devirtualizer() {
        @Override
        public boolean shouldBeDevirtualized(
            MethodCall methodCall,
            IMethodBinding methodBinding,
            org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
          // Do not devirtualize inside the same declaring class, because it does not need to go
          // throug the trampoline path in Booleans functions.
          return JdtUtils.isBooleanInstanceMethodBinding(methodBinding, compilationUnit)
              && dispatchesToSomeOtherClass(methodCall);
        }

        @Override
        public TypeDescriptor getEnclosingClassTypeDescriptor() {
          return TypeDescriptors.BOOLEANS_TYPE_DESCRIPTOR;
        }

        @Override
        public TypeDescriptor getInstanceTypeDescriptor() {
          return TypeDescriptors.boxedTypeByPrimitiveType.get(
              TypeDescriptors.BOOLEAN_TYPE_DESCRIPTOR);
        }
      };

  private static Devirtualizer comparableDevirtualizer =
      new Devirtualizer() {
        @Override
        public boolean shouldBeDevirtualized(
            MethodCall methodCall,
            IMethodBinding methodBinding,
            org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
          // Do not devirtualize inside the same declaring class, because it does not need to go
          // throug the trampoline path in Comparables functions.
          return JdtUtils.isComparableInstanceMethodBinding(methodBinding)
              && dispatchesToSomeOtherClass(methodCall);
        }

        @Override
        public TypeDescriptor getEnclosingClassTypeDescriptor() {
          return TypeDescriptors.COMPARABLES_TYPE_DESCRIPTOR;
        }

        @Override
        public TypeDescriptor getInstanceTypeDescriptor() {
          return TypeDescriptors.COMPARABLE_TYPE_DESCRIPTOR;
        }
      };

  private static boolean dispatchesToSomeOtherClass(MethodCall methodCall) {
    Expression qualifier = methodCall.getQualifier();
    return qualifier != null && !(qualifier instanceof ThisReference);
  }

  private static List<Devirtualizer> devirtualizers =
      Arrays.<Devirtualizer>asList(
          objectDevirtualizer, numberDevirtualizer, booleanDevirtualizer, comparableDevirtualizer);

  public static MethodCall doDevirtualization(
      MethodCall methodCall,
      IMethodBinding methodBinding,
      org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
    for (Devirtualizer devirtualizer : devirtualizers) {
      if (devirtualizer.shouldBeDevirtualized(methodCall, methodBinding, compilationUnit)) {
        return ASTUtils.createDevirtualizedMethodCall(
            methodCall,
            devirtualizer.getEnclosingClassTypeDescriptor(),
            devirtualizer.getInstanceTypeDescriptor());
      }
    }
    return methodCall;
  }
}
