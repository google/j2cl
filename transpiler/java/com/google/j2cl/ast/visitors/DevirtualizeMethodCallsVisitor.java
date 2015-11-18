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

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Devirtualizes method calls to instance methods of Object, unboxed types (Boolean, Double, String)
 * and their super classes and super interfaces to corresponding static method calls.
 */
public class DevirtualizeMethodCallsVisitor extends AbstractRewriter {
  public static void applyTo(CompilationUnit compilationUnit) {
    new DevirtualizeMethodCallsVisitor().devirtualizeMethodCalls(compilationUnit);
  }

  private void devirtualizeMethodCalls(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  @Override
  public Node rewriteMethodCall(MethodCall methodCall) {
    MethodDescriptor targetMethodDescriptor = methodCall.getTarget();
    if (targetMethodDescriptor.isStatic()
        || targetMethodDescriptor.isConstructor()
        || targetMethodDescriptor.isJsProperty() // never devirtualize JsProperty method.
        || targetMethodDescriptor.isInit()) { // do not devirtualize the synthesized $init method.
      return methodCall;
    }
    return doDevirtualization(methodCall);
  }

  /**
   * Mapping from the TypeDescriptor, whose instance methods should be devirtualized, to the
   * TypeDescriptor that contains the devirtualized static methods that should be dispatched to.
   *
   * <p>The instance methods of unboxed types (Boolean, Double, String) are translated to
   * corresponding static methods that are translated automatically by J2cl. Instance methods of
   * Object and the super classes/interfaces of unboxed types are translated to the trampoline
   * methods which are implemented in corresponding types (Objects, Numbers, etc.).
   */
  private Map<TypeDescriptor, TypeDescriptor> devirtualizationTypeDescriptorsMapping =
      new LinkedHashMap<>();

  {
    devirtualizationTypeDescriptorsMapping.put(
        TypeDescriptors.get().javaLangObject, BootstrapType.OBJECTS.getDescriptor());
    devirtualizationTypeDescriptorsMapping.put(
        TypeDescriptors.get().javaLangBoolean, TypeDescriptors.get().javaLangBoolean);
    devirtualizationTypeDescriptorsMapping.put(
        TypeDescriptors.get().javaLangDouble, TypeDescriptors.get().javaLangDouble);
    devirtualizationTypeDescriptorsMapping.put(
        TypeDescriptors.get().javaLangString, TypeDescriptors.get().javaLangString);
    devirtualizationTypeDescriptorsMapping.put(
        TypeDescriptors.get().javaLangNumber, BootstrapType.NUMBERS.getDescriptor());
    devirtualizationTypeDescriptorsMapping.put(
        TypeDescriptors.get().javaLangComparable.getRawTypeDescriptor(),
        BootstrapType.COMPARABLES.getDescriptor());
    devirtualizationTypeDescriptorsMapping.put(
        TypeDescriptors.get().javaLangCharSequence, BootstrapType.CHAR_SEQUENCES.getDescriptor());
  }

  private MethodCall doDevirtualization(MethodCall methodCall) {
    // Do *not* perform Object method devirtualization. The point with super method calls is to
    // *not* call the default version of the method on the prototype and instead call the specific
    // version of the method in the super class. If we were to perform Object method
    // devirtualization then the resulting routing through Objects.doFoo() would end up calling
    // back onto the version of the method on the prototype (aka the wrong one).
    if (methodCall.getQualifier() instanceof SuperReference) {
      return methodCall;
    }
    TypeDescriptor enclosingClassTypeDescriptor =
        methodCall.getTarget().getEnclosingClassTypeDescriptor().getRawTypeDescriptor();
    for (Entry<TypeDescriptor, TypeDescriptor> entry :
        devirtualizationTypeDescriptorsMapping.entrySet()) {
      if (enclosingClassTypeDescriptor == entry.getKey()) {
        return AstUtils.createDevirtualizedMethodCall(methodCall, entry.getValue(), entry.getKey());
      }
    }
    return methodCall;
  }
}
