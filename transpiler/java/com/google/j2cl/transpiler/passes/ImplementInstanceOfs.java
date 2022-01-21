/*
 * Copyright 2020 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;

/** Implements the static $isInstance methods used to support Java instanceof semantics. */
public class ImplementInstanceOfs extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
      if (type.getUnderlyingTypeDeclaration().isNoopCast()
          || type.getUnderlyingTypeDeclaration().isJsFunctionInterface()) {
      // isInstance for JsFunction interfaces is implemented in the runtime object
      // vmboostrap.JavaScriptFunction, so there is no need to generate any of these methods in
      // the overlay.
      return;
      }

      synthesizeMarkImplementor(type);
      synthesizeIsInstanceMethod(type);
      synthesizeMarkImplementorCalls(type);
  }

  /**
   * Synthesizes load time calls to $markImplementor methods to add the isInstance support marker
   * fields.
   */
  private static void synthesizeMarkImplementorCalls(Type type) {
    if (type.isOverlayImplementation()) {
      return;
    }
    if (type.isInterface()) {
      // Call markImplementor on the interface to be able to do casts and instanceOf on arrays.
      // Note that the implementation of markImplementor will recursively call markImplementor on
      // super interfaces.

      // Type.$markImplementor(Type);
      type.addLoadTimeStatement(
          createMarkImplementorCall(
              type.getTypeDescriptor(),
              // TODO(b/79389970): remove cast after b/79389970 is handled in Closure.
              JsDocCastExpression.newBuilder()
                  .setCastType(TypeDescriptors.get().nativeFunction)
                  .setExpression(new JavaScriptConstructorReference(type.getDeclaration()))
                  .build()));
    } else {
      // Call markImplementor on all interfaces that are directly implemented by the class to
      // implement the support instanceOf Interface.
      // The implementation of markImplementor in the interfaces is responsible for calling
      // markImplementor on their super interfaces.

      for (DeclaredTypeDescriptor interfaceTypeDescriptor :
          type.getSuperInterfaceTypeDescriptors()) {
        if (interfaceTypeDescriptor.isNoopCast()) {
          continue;
        }
        // Interface.$markImplementor(Type);
        type.addLoadTimeStatement(
            createMarkImplementorCall(
                interfaceTypeDescriptor,
                new JavaScriptConstructorReference(type.getDeclaration())));
      }
    }
  }

  private static ExpressionStatement createMarkImplementorCall(
      DeclaredTypeDescriptor implementedInterface, Expression constructor) {
    return MethodCall.Builder.from(implementedInterface.getMarkImplementorMethodDescriptor())
        .setArguments(constructor)
        .build()
        .makeStatement(SourcePosition.NONE);
  }

  /** Synthesizes the $isInstance method on the type. */
  private static void synthesizeIsInstanceMethod(Type type) {
    if (type.containsMethod(MethodDescriptor.IS_INSTANCE_METHOD_NAME)) {
      // User provided an $isInstance method, skip generation.
      return;
    }

    Variable instanceParameter =
        Variable.newBuilder()
            .setName("instance")
            .setTypeDescriptor(TypeDescriptors.get().javaLangObject)
            .setParameter(true)
            .setFinal(true)
            .build();

    // $isInstance(instance) {
    //    return <expression for instanceOf>.
    // }
    type.addMember(
        Method.newBuilder()
            .setMethodDescriptor(type.getTypeDescriptor().getIsInstanceMethodDescriptor())
            .setParameters(instanceParameter)
            .addStatements(
                ReturnStatement.newBuilder()
                    .setExpression(synthesizeIsInstanceExpression(instanceParameter, type))
                    .setSourcePosition(SourcePosition.NONE)
                    .build())
            .setSourcePosition(SourcePosition.NONE)
            .build());
  }

  /** Return the appropriate expression to check isInstance depending on the target type. */
  private static Expression synthesizeIsInstanceExpression(Variable instance, Type type) {
    if (type.isOverlayImplementation() && type.getOverlaidTypeDeclaration().isJsEnum()) {
      return synthesizeInstanceOfJsEnum(instance, type);
    } else if (type.getDeclaration().isJsFunctionImplementation() || type.isInterface()) {
      return synthesizeInstanceOfJavaInterface(instance, type);
    } else {
      return synthesizeInstanceOfClass(instance, type);
    }
  }

  /** Synthesizes the $isInstance method directly using JavaScript instanceof. */
  private static Expression synthesizeInstanceOfClass(Variable instance, Type type) {
    // instance instanceof Type.
    return InstanceOfExpression.newBuilder()
        .setExpression(instance.createReference())
        .setTestTypeDescriptor(
            type.getUnderlyingTypeDeclaration().toUnparameterizedTypeDescriptor())
        .build();
  }

  /** Synthesizes the $isInstance that checks the presence of the corresponding marker field. */
  private static Expression synthesizeInstanceOfJavaInterface(Variable instance, Type type) {
    // instance != null && !!instance.$implements_Interface.
    return instance
        .createReference()
        .infixNotEqualsNull()
        .infixAnd(
            FieldAccess.Builder.from(type.getTypeDescriptor().getIsInstanceMarkerField())
                .setQualifier(instance.createReference())
                .build()
                .prefixNot()
                .prefixNot());
  }

  /** Synthesize the $isInstance check for JsEnums. */
  private static Expression synthesizeInstanceOfJsEnum(Variable instance, Type type) {

    TypeDeclaration typeDeclaration = type.getUnderlyingTypeDeclaration();
    if (typeDeclaration.isNative()) {
      //  Native enums are never boxed and always represented as values of the underlying type,
      //  hence just delegate the isInstance check to the value type of the enum.
      checkState(typeDeclaration.getJsEnumInfo().hasCustomValue());
      DeclaredTypeDescriptor instanceOfValueType =
          AstUtils.getJsEnumValueFieldInstanceCheckType(typeDeclaration);

      //   ValueType.$isInstance(instance)
      return MethodCall.Builder.from(instanceOfValueType.getIsInstanceMethodDescriptor())
          .setArguments(instance.createReference())
          .build();
    }
    // Non native JsEnums will always appear as instance of BoxedLightEnum in instanceOf and casts
    // expressions, so delegate the isInstance check to a custom method in the runtime class Enums.
    // TODO(b/118299062): this needs to be changed for supporting arrays of JsEnums.
    //   $Enums.isInstanceOf(instance, Type)
    return RuntimeMethods.createEnumsMethodCall(
        "isInstanceOf",
        instance.createReference(),
        typeDeclaration.toUnparameterizedTypeDescriptor().getMetadataConstructorReference());
  }

  /**
   * Synthesizes the $markImplementor methods that adds all the necessary type markers to a type, in
   * order to support instanceOf JavaInterface.
   */
  private static void synthesizeMarkImplementor(Type type) {
    if (!type.isInterface()) {
      // Only interfaces need $markImplementor methods. All other kind of types use a different
      // mechanism to implement isInstance with most of them relying on the native JavaScript
      // isntanceof Constructor operation.
      return;
    }

    Variable ctorParameter =
        Variable.newBuilder()
            .setName("ctor")
            .setTypeDescriptor(TypeDescriptors.get().nativeFunction)
            .setParameter(true)
            .setFinal(true)
            .build();
    Method.Builder methodBuilder =
        Method.newBuilder()
            .setMethodDescriptor(type.getTypeDescriptor().getMarkImplementorMethodDescriptor())
            .setParameters(ctorParameter)
            .setSourcePosition(SourcePosition.NONE);

    // Call markImplementor on super interfaces to recursively add all the interface type markers.
    for (DeclaredTypeDescriptor superInterface : type.getSuperInterfaceTypeDescriptors()) {
      if (superInterface.isNative()) {
        continue;
      }
      /** SuperInterface.$markImplementor(ctor); */
      methodBuilder.addStatements(
          createMarkImplementorCall(superInterface, ctorParameter.createReference()));
    }

    // And finally add the marker corresponding to the current interface type.
    /** ctor.prototype.$implements_Type = true. */
    methodBuilder.addStatements(
        BinaryExpression.Builder.asAssignmentTo(
                FieldAccess.Builder.from(type.getTypeDescriptor().getIsInstanceMarkerField())
                    .setQualifier(ctorParameter.createReference().getPrototypeFieldAccess())
                    .build())
            .setRightOperand(BooleanLiteral.get(true))
            .build()
            .makeStatement(SourcePosition.NONE));

    type.addMember(methodBuilder.build());
  }
}
