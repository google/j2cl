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

import com.google.j2cl.ast.AstUtils;
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
          return JdtUtils.isObjectInstanceMethodBinding(methodBinding, compilationUnit);
        }

        @Override
        public TypeDescriptor getEnclosingClassTypeDescriptor() {
          return TypeDescriptors.OBJECTS_TYPE_DESCRIPTOR;
        }

        @Override
        public TypeDescriptor getInstanceTypeDescriptor() {
          return TypeDescriptors.get().javaLangObject;
        }
      };

  private static Devirtualizer numberDevirtualizer =
      new Devirtualizer() {
        @Override
        public boolean shouldBeDevirtualized(
            MethodCall methodCall,
            IMethodBinding methodBinding,
            org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
          return JdtUtils.isNumberInstanceMethodBinding(methodBinding, compilationUnit);
        }

        @Override
        public TypeDescriptor getEnclosingClassTypeDescriptor() {
          return TypeDescriptors.NUMBERS_TYPE_DESCRIPTOR;
        }

        @Override
        public TypeDescriptor getInstanceTypeDescriptor() {
          return TypeDescriptors.get().javaLangNumber;
        }
      };

  private static Devirtualizer booleanDevirtualizer =
      new Devirtualizer() {
        @Override
        public boolean shouldBeDevirtualized(
            MethodCall methodCall,
            IMethodBinding methodBinding,
            org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
          return JdtUtils.isBooleanInstanceMethodBinding(methodBinding, compilationUnit);
        }

        @Override
        public TypeDescriptor getEnclosingClassTypeDescriptor() {
          return TypeDescriptors.BOOLEANS_TYPE_DESCRIPTOR;
        }

        @Override
        public TypeDescriptor getInstanceTypeDescriptor() {
          return TypeDescriptors.get().javaLangBoolean;
        }
      };

  private static Devirtualizer comparableDevirtualizer =
      new Devirtualizer() {
        @Override
        public boolean shouldBeDevirtualized(
            MethodCall methodCall,
            IMethodBinding methodBinding,
            org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
          return JdtUtils.isComparableInstanceMethodBinding(methodBinding);
        }

        @Override
        public TypeDescriptor getEnclosingClassTypeDescriptor() {
          return TypeDescriptors.COMPARABLES_TYPE_DESCRIPTOR;
        }

        @Override
        public TypeDescriptor getInstanceTypeDescriptor() {
          return TypeDescriptors.get().javaLangComparable;
        }
      };

  private static Devirtualizer charSequenceDevirtualizer =
      new Devirtualizer() {
        @Override
        public boolean shouldBeDevirtualized(
            MethodCall methodCall,
            IMethodBinding methodBinding,
            org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
          return JdtUtils.isCharSequenceInstanceMethodBinding(methodBinding);
        }

        @Override
        public TypeDescriptor getEnclosingClassTypeDescriptor() {
          return TypeDescriptors.CHAR_SEQUENCES_TYPE_DESCRIPTOR;
        }

        @Override
        public TypeDescriptor getInstanceTypeDescriptor() {
          return TypeDescriptors.get().javaLangCharSequence;
        }
      };

  private static Devirtualizer stringDevirtualizer =
      new Devirtualizer() {
        @Override
        public boolean shouldBeDevirtualized(
            MethodCall methodCall,
            IMethodBinding methodBinding,
            org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {
          return JdtUtils.isStringInstanceMethodBinding(methodBinding);
        }

        @Override
        public TypeDescriptor getEnclosingClassTypeDescriptor() {
          return TypeDescriptors.STRINGS_TYPE_DESCRIPTOR;
        }

        @Override
        public TypeDescriptor getInstanceTypeDescriptor() {
          return TypeDescriptors.get().javaLangString;
        }
      };

  private static List<Devirtualizer> devirtualizers =
      Arrays.<Devirtualizer>asList(
          objectDevirtualizer,
          numberDevirtualizer,
          booleanDevirtualizer,
          comparableDevirtualizer,
          charSequenceDevirtualizer,
          stringDevirtualizer);

  public static MethodCall doDevirtualization(
      MethodCall methodCall,
      IMethodBinding methodBinding,
      org.eclipse.jdt.core.dom.CompilationUnit compilationUnit) {

    Expression qualifier = methodCall.getQualifier();
    if (!JdtUtils.isInstanceMethod(methodBinding) || (qualifier instanceof ThisReference)) {
      /**
       * We don't want to devirtualize static method calls.
       *
       * We don't want to devirtualize calls to 'this' because we know 'this' is a subclass of
       * Object, we don't need to go through the trampoline path in Object to determine that its
       * not one of the devirtualized types. See Objects.impl.js. Since this is an optimization
       * we might want to allow the js compiler to do this in the future.
       */
      return methodCall;
    }

    for (Devirtualizer devirtualizer : devirtualizers) {
      if (devirtualizer.shouldBeDevirtualized(methodCall, methodBinding, compilationUnit)) {
        return AstUtils.createDevirtualizedMethodCall(
            methodCall,
            devirtualizer.getEnclosingClassTypeDescriptor(),
            devirtualizer.getInstanceTypeDescriptor());
      }
    }
    return methodCall;
  }
}
