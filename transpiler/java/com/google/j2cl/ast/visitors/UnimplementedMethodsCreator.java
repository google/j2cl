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

import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import java.util.List;

/**
 * Creates unimplemented methods in abstract class.
 *
 * <p>An abstract class in Java may have unimplemented methods that are declared by its super
 * interfaces. However, in JavaScript, it requires a class tagged with 'implements' to implement all
 * of the methods defined by the interface. This class synthesizes the unimplemented methods in an
 * abstract class.
 */
public class UnimplementedMethodsCreator extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new UnimplementedMethodsCreatorVisitor());
  }

  private static class UnimplementedMethodsCreatorVisitor extends AbstractVisitor {
    @Override
    public boolean enterType(Type type) {
      // Only stub methods on abstract classes.
      if (!type.isAbstract()) {
        return true;
      }
      for (MethodDescriptor methodToStub : type.getDescriptor().getAllMethods()) {
        if (methodToStub.getEnclosingClassTypeDescriptor().isInterface()
            && !methodToStub.isConstructor()
            && !methodToStub.isStatic()) {
          type.addMethod(
              createEmptyMethod(
                  MethodDescriptor.Builder.from(methodToStub)
                      .setEnclosingClassTypeDescriptor(type.getDescriptor())
                      .build()));
        }
      }
      return true; // Need to visit inner classes.
    }
  }

  /**
   * Returns an empty method in type {@code typeBinding} that has the same signature of {@code
   * methodBinding}.
   */
  private static Method createEmptyMethod(MethodDescriptor methodDescriptor) {
    List<Variable> parameters = Lists.newArrayList();
    int index = 0;
    for (TypeDescriptor type : methodDescriptor.getParameterTypeDescriptors()) {
      parameters.add(new Variable("arg" + index++, type, false, true));
    }
    return Method.Builder.fromDefault()
        .setMethodDescriptor(methodDescriptor)
        .setParameters(parameters)
        .setIsAbstract(true)
        .setIsOverride(true)
        .setIsFinal(false)
        .build();
  }
}
