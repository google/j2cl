/*
 * Copyright 2016 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.InitializerBlock;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates Overlay types, bridges to overlay methods and redirects member accesses:
 *
 * <p>Creates Overlay types (by moving fields and methods from the original type to the new Overlay
 * type) for JsTypes that have fields or methods and for Interfaces that have default methods.
 *
 * <p>Any references to the moved fields and methods are redirected to the new location.
 *
 * <p>When moving instance methods out of a JsType, those methods must be devirtualized. As a result
 * reference sites must be converted from instance method calls to static method calls. This
 * inappropriately creates new imports not present in the original Java source since the class that
 * implements the static function must be imported. To avoid these imports and to avoid making
 * strict dependency checkers unhappy we sometimes synthesize bridge methods and sometimes carefully
 * dispatch static method calls to take advantage of static method inheritance in ES6 classes.
 *
 * <p>Take the following scenario:
 *
 * <ul>
 * <li>@JsType(isNative=true) class A { @JsOverlay void fooMethod() {} }
 * <li>@JsType(isNative=true) class B extends A {}
 * <li>class C extends B {}
 * <li>class D extends C {}
 * </ul>
 *
 * <p>A's "fooMethod" should be moved to A$Overlay and instance like "a.fooMethod()" should be
 * rewritten as "A$Overlay.fooMethod(a)".
 *
 * <p>B's B$Overlay class should contain no methods and no bridge either but it will be emitted to
 * extend A$Overlay. Instance dispatches like "b.fooMethod()" should be rewritten as
 * "B$Overlay.fooMethod(b)", taking advantage of static method inheritance in ES6 classes.
 *
 * <p>C has an impl class instead of an overlay class. A bridge method named "fooMethod" should
 * added in C that calls "B$Overlay.fooMethod(this)" and instance dispatches like "c.fooMethod()"
 * should be left as they are.
 *
 * <p>D needs no changes since it inherits the bridge added in C. Instance dispatches like
 * "d.fooMethod()" should be left as they are.
 */
public class CreateOverlayImplementationTypesAndDevirtualizeCalls extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    OverlayImplementationTypesCreator.applyTo(compilationUnit);
    OverlayBridgesCreator.applyTo(compilationUnit);
    compilationUnit.accept(new MemberAccessesRedirector());
  }

  /**
   * Finds regular classes that directly subclass a native JsType class, then finds all JsOverlay
   * methods in the super type hierarchy that are expected to exist in synthesized Overlay classes,
   * and then creates an instance method bridge to the overlay method. The existence of these
   * bridges will make it possible to dispatch at method call sites without creating imports that
   * weren't present in the original Java source.
   */
  private static class OverlayBridgesCreator {

    static void addBridges(
        Type type, TypeDescriptor typeDescriptor, TypeDescriptor superTypeDescriptor) {
      for (MethodDescriptor methodDescriptor : superTypeDescriptor.getMethodDescriptors()) {
        // The only methods that need a bridge are JsOverlay methods that will be moved to the
        // Overlay class. This does not include JsProperty(s) since they are not moved to the
        // Overlay class.
        if (!methodDescriptor.isJsOverlay()) {
          continue;
        }

        type.addMethod(createBridgeMethod(typeDescriptor, methodDescriptor));
      }
    }

    static void applyTo(CompilationUnit compilationUnit) {
      for (Type type : compilationUnit.getTypes()) {
        TypeDescriptor typeDescriptor = type.getDescriptor();
        TypeDescriptor superTypeDescriptor = typeDescriptor.getSuperTypeDescriptor();
        // The only classes that should have bridges added are regular classes that are the
        // immediate subclass a native JsType class.
        if (typeDescriptor.isNative()
            || superTypeDescriptor == null
            || !superTypeDescriptor.isNative()) {
          continue;
        }

        addBridges(type, typeDescriptor, superTypeDescriptor);
      }
    }

    static Method createBridgeMethod(
        TypeDescriptor typeDescriptor, MethodDescriptor targetMethodDescriptor) {
      MethodDescriptor bridgeMethodDescriptor =
          MethodDescriptor.Builder.from(targetMethodDescriptor)
              .setEnclosingClassTypeDescriptor(typeDescriptor)
              .setJsInfo(JsInfo.NONE)
              .setNative(false)
              .setSynthetic(true)
              .setBridge(true)
              .build();

      return AstUtils.createForwardingMethod(
          new ThisReference(typeDescriptor.getSuperTypeDescriptor()),
          bridgeMethodDescriptor,
          targetMethodDescriptor,
          "Instance bridge to super overlay method.",
          false);
    }
  }

  /**
   * Finds classes that need a matching overlay class (either because the class is a native JsType
   * or because it is an interface with default methods) and creates the overlay class by copying
   * over devirtualized versions of the methods in question.
   */
  private static class OverlayImplementationTypesCreator {

    static void applyTo(CompilationUnit compilationUnit) {
      List<Type> replacementTypeList = new ArrayList<>();

      // TODO(goktug): we should actually do proper rewrite of the nodes. Current code leaves
      // traces of overlay members in the original class that requires us cloning here.
      for (Type type : compilationUnit.getTypes()) {
        if (!type.isNative()) {
          replacementTypeList.add(type);
        }

        if (type.getDescriptor().isNative() || type.getDescriptor().declaresDefaultMethods()) {
          replacementTypeList.add(createOverlayImplementationType(type));
        }
      }
      compilationUnit.getTypes().clear();
      compilationUnit.getTypes().addAll(replacementTypeList);
    }

    static Type createOverlayImplementationType(Type type) {
      TypeDescriptor overlayImplTypeDescriptor =
          TypeDescriptors.createOverlayImplementationClassTypeDescriptor(type.getDescriptor());
      Type overlayClass = new Type(type.getVisibility(), overlayImplTypeDescriptor);
      overlayClass.setNativeTypeDescriptor(type.getDescriptor());

      for (Member member : type.getMembers()) {
        if (member instanceof Method) {
          Method method = (Method) member;
          if (!method.getDescriptor().isJsOverlay() && !method.getDescriptor().isDefault()) {
            continue;
          }
          overlayClass.addMethod(createOverlayMethod(method, overlayImplTypeDescriptor));
        } else if (member instanceof Field) {
          Field field = (Field) member;
          if (!field.getDescriptor().isJsOverlay()) {
            continue;
          }
          checkState(field.getDescriptor().isStatic());
          overlayClass.addField(
              Field.Builder.from(field)
                  .setInitializer(AstUtils.clone(field.getInitializer()))
                  .setEnclosingClass(overlayImplTypeDescriptor)
                  .build());
        } else {
          InitializerBlock initializerBlock = (InitializerBlock) member;
          checkState(initializerBlock.isStatic());
          overlayClass.addStaticInitializerBlock(AstUtils.clone(initializerBlock.getBlock()));
          initializerBlock.getBlock().getStatements().clear();
        }
      }

      return overlayClass;
    }

    static Method createOverlayMethod(Method method, TypeDescriptor overlayImplTypeDescriptor) {
      Method statifiedMethod =
          method.getDescriptor().isStatic() ? method : AstUtils.createDevirtualizedMethod(method);
      Method movedMethod =
          Method.Builder.from(statifiedMethod)
              .setJsInfo(JsInfo.NONE)
              .setEnclosingClass(overlayImplTypeDescriptor)
              .build();

      // clear the method body from the original type and use a fresh list of parameters.
      method.getBody().getStatements().clear();
      List<Variable> newParameters = AstUtils.clone(method.getParameters());
      method.getParameters().clear();
      method.getParameters().addAll(newParameters);

      return movedMethod;
    }
  }

  /**
   * Finds accesses to properties that have been moved to overlay classes, and rewrites the accesses
   * to the new location. So as to avoid introducing new dependencies, these rewritten accesses take
   * advantage of overlay class static method inheritance, normal-to-native class bridge methods,
   * and normal class instance method inheritance.
   */
  private static class MemberAccessesRedirector extends AbstractRewriter {

    static Node createRedirectedAccessToOverlayClass(FieldAccess fieldAccess) {
      checkArgument(fieldAccess.getTarget().isStatic());

      FieldDescriptor targetFieldDescriptor = fieldAccess.getTarget();
      TypeDescriptor overlayTypeDescriptor =
          TypeDescriptors.createOverlayImplementationClassTypeDescriptor(
              targetFieldDescriptor.getEnclosingClassTypeDescriptor());
      return FieldAccess.Builder.from(
              FieldDescriptor.Builder.from(targetFieldDescriptor)
                  .setEnclosingClassTypeDescriptor(overlayTypeDescriptor)
                  .build())
          .build();
    }

    static Node createRedirectedStaticMethodCall(
        MethodCall methodCall, TypeDescriptor overlayTypeDescriptor) {
      return MethodCall.Builder.from(methodCall)
          .setEnclosingClass(overlayTypeDescriptor)
          .setQualifier(null)
          .build();
    }

    @Override
    public Node rewriteFieldAccess(FieldAccess fieldAccess) {
      if (fieldAccess.getTarget().isJsOverlay()) {
        return createRedirectedAccessToOverlayClass(fieldAccess);
      }

      return fieldAccess;
    }

    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      TypeDescriptor enclosingClassTypeDescriptor =
          methodCall.getTarget().getEnclosingClassTypeDescriptor();
      boolean targetIsJsOverlayInNativeClass =
          (enclosingClassTypeDescriptor.isNative()
                  || enclosingClassTypeDescriptor.isJsFunctionInterface())
              && methodCall.getTarget().isJsOverlay();
      boolean targetIsDefaultMethodAccessedStatically =
          methodCall.getTarget().isDefault() && methodCall.isStaticDispatch();

      if (targetIsJsOverlayInNativeClass || targetIsDefaultMethodAccessedStatically) {
        TypeDescriptor targetOverlayTypeDescriptor =
            TypeDescriptors.createOverlayImplementationClassTypeDescriptor(
                enclosingClassTypeDescriptor);

        if (methodCall.getTarget().isStatic()) {
          // Call already-static method directly at their new overlay class location.
          return createRedirectedStaticMethodCall(methodCall, targetOverlayTypeDescriptor);
        } else if (methodCall.getTarget().isDefault()) {
          // Call default methods statically at their new overlay class location.
          return AstUtils.createDevirtualizedMethodCall(methodCall, targetOverlayTypeDescriptor);
        } else if (methodCall.getQualifier().getTypeDescriptor().isNative()) {
          // If the current instance type has an overlay class then call overlay methods statically
          // on that overlay class, they will prototypically resolve to the final location.
          TypeDescriptor instanceOverlayTypeDescriptor =
              TypeDescriptors.createOverlayImplementationClassTypeDescriptor(
                  methodCall.getQualifier().getTypeDescriptor());
          return AstUtils.createDevirtualizedMethodCall(methodCall, instanceOverlayTypeDescriptor);
        } else {
          // If the current instance type does not have an overlay class then leave the call alone.
          // It will prototypically resolve to the overlay bridge method created earlier.
          return methodCall;
        }
      }

      return methodCall;
    }
  }
}
