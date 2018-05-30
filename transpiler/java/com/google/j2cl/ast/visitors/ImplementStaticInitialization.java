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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.FieldDescriptor.FieldOrigin;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.JsMemberType;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.PrimitiveTypes;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.common.SourcePosition;

/**
 * Implements static initialization by synthesizing getter and setters for static fields and
 * instrumenting static members so that static initialization (clinit) is triggered according to
 * Java semantics.
 */
public class ImplementStaticInitialization extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    for (Type type : compilationUnit.getTypes()) {
      insertClinitCalls(type);
      synthesizeSuperClinitCalls(type);
      synthesizeSettersAndGetters(type);
    }
  }

  /** Add clinit calls to static methods and (real js) constructors. */
  private void insertClinitCalls(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Method rewriteMethod(Method method) {
            if (needsClinitCall(method.getDescriptor())) {
              return Method.Builder.from(method)
                  .addStatement(
                      0,
                      createClinitCallStatement(
                          method.getBody().getSourcePosition(),
                          method.getDescriptor().getEnclosingTypeDescriptor()))
                  .build();
            }
            return method;
          }
        });
  }

  /** Synthesize a static initializer block that calls the necessary super type clinits. */
  private void synthesizeSuperClinitCalls(Type type) {
    Block.Builder staticInitializerBuilder =
        Block.newBuilder().setSourcePosition(type.getSourcePosition());

    if (hasClinitMethod(type.getSuperTypeDescriptor())) {
      staticInitializerBuilder.addStatement(
          createClinitCallStatement(type.getSourcePosition(), type.getSuperTypeDescriptor()));
    }
    addRequiredSuperInterfacesClinitCalls(
        type.getSourcePosition(), type.getTypeDescriptor(), staticInitializerBuilder);

    Block staticInitiliazerBlock = staticInitializerBuilder.build();
    if (!staticInitiliazerBlock.isEmpty()) {
      type.addStaticInitializerBlock(0, staticInitiliazerBlock);
    }
  }

  /**
   * Synthesize static getters/setter to ensure class initialization is run on static field
   * accesses.
   */
  private void synthesizeSettersAndGetters(Type type) {
    for (Field staticField : type.getStaticFields()) {
      if (!triggersClinit(staticField.getDescriptor())) {
        continue;
      }
      synthesizePropertyGetter(type, staticField);
      synthesizePropertySetter(type, staticField);
    }

    // Replace the actual static fields with their backing fields.
    type.accept(
        new AbstractRewriter() {
          @Override
          public Member rewriteField(Field field) {
            if (!triggersClinit(field.getDescriptor())) {
              return field;
            }
            return Field.Builder.from(getBackingFieldDescriptor(field.getDescriptor()))
                .setInitializer(field.getInitializer())
                .setSourcePosition(field.getSourcePosition())
                .build();
          }
        });

    // Replace field access within the class for accesses to the backing field.
    type.accept(
        new AbstractRewriter() {
          @Override
          public FieldAccess rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor fieldDescriptor = fieldAccess.getTarget();
            if (!triggersClinit(fieldDescriptor)) {
              return fieldAccess;
            }

            if (!fieldDescriptor.isMemberOf(type.getDeclaration())) {
              return fieldAccess;
            }

            return FieldAccess.Builder.from(getBackingFieldDescriptor(fieldDescriptor)).build();
          }
        });
  }

  private static boolean triggersClinit(FieldDescriptor fieldDescriptor) {
    return fieldDescriptor.isStatic() && !fieldDescriptor.isCompileTimeConstant();
  }

  public void synthesizePropertyGetter(Type type, Field field) {
    FieldDescriptor fieldDescriptor = field.getDescriptor();
    type.addMethod(
        Method.newBuilder()
            .setSourcePosition(field.getSourcePosition())
            .setMethodDescriptor(getGetterMethodDescriptor(fieldDescriptor))
            .addStatements(
                AstUtils.createReturnOrExpressionStatement(
                    field.getSourcePosition(),
                    MultiExpression.newBuilder()
                        .addExpressions(
                            createClinitCallExpression(
                                fieldDescriptor.getEnclosingTypeDescriptor()),
                            FieldAccess.Builder.from(fieldDescriptor).build())
                        .build(),
                    fieldDescriptor.getTypeDescriptor()))
            .build());
  }

  public void synthesizePropertySetter(Type type, Field field) {
    FieldDescriptor fieldDescriptor = field.getDescriptor();
    Variable parameter =
        Variable.newBuilder()
            .setName("value")
            .setTypeDescriptor(fieldDescriptor.getTypeDescriptor())
            .setParameter(true)
            .build();
    type.addMethod(
        Method.newBuilder()
            .setSourcePosition(field.getSourcePosition())
            .setMethodDescriptor(getSetterMethodDescriptor(fieldDescriptor))
            .setParameters(parameter)
            .addStatements(
                MultiExpression.newBuilder()
                    .addExpressions(
                        createClinitCallExpression(fieldDescriptor.getEnclosingTypeDescriptor()),
                        BinaryExpression.Builder.asAssignmentTo(fieldDescriptor)
                            .setRightOperand(parameter)
                            .build())
                    .build()
                    .makeStatement(field.getSourcePosition()))
            .build());
  }

  private static Statement createClinitCallStatement(
      SourcePosition sourcePosition, DeclaredTypeDescriptor typeDescriptor) {
    return createClinitCallExpression(typeDescriptor).makeStatement(sourcePosition);
  }

  private static Expression createClinitCallExpression(DeclaredTypeDescriptor typeDescriptor) {
    MethodDescriptor clinitMethodDescriptor = AstUtils.getClinitMethodDescriptor(typeDescriptor);
    return MethodCall.Builder.from(clinitMethodDescriptor).build();
  }

  private void addRequiredSuperInterfacesClinitCalls(
      SourcePosition sourcePosition,
      DeclaredTypeDescriptor typeDescriptor,
      Block.Builder staticInitializerBuilder) {
    for (DeclaredTypeDescriptor interfaceTypeDescriptor :
        typeDescriptor.getInterfaceTypeDescriptors()) {
      if (!hasClinitMethod(interfaceTypeDescriptor)) {
        continue;
      }

      if (interfaceTypeDescriptor.getTypeDeclaration().declaresDefaultMethods()) {
        // The interface declares a default method; invoke its clinit which will initialize
        // the interface and all it superinterfaces that declare default methods.
        staticInitializerBuilder.addStatement(
            createClinitCallStatement(sourcePosition, interfaceTypeDescriptor));
        continue;
      }

      // The interface does not declare a default method so don't call its clinit; instead recurse
      // into its super interface hierarchy to invoke clinits of super interfaces that declare
      // default methods.
      addRequiredSuperInterfacesClinitCalls(
          sourcePosition, interfaceTypeDescriptor, staticInitializerBuilder);
    }
  }

  private static boolean needsClinitCall(MethodDescriptor methodDescriptor) {
    // Skip native methods.
    if (methodDescriptor.isNative()) {
      return false;
    }

    // At this point all constructors (except JsConstructors) are static factory ($create) methods,
    // so clinits need to be inserted only on static methods or JsConstructors. clinits are not
    // needed in factories or JsConstructors for enum classes because those are already only called
    // from the clinit itself.

    if (methodDescriptor.getEnclosingTypeDescriptor().isEnum()
        && methodDescriptor.getOrigin() == MethodOrigin.SYNTHETIC_FACTORY_FOR_CONSTRUCTOR) {
      // These are the classes corresponding to enums excluding the anonymous enum subclasses.
      return false;
    }

    return methodDescriptor.isStatic() || methodDescriptor.isJsConstructor();
  }

  private static boolean hasClinitMethod(TypeDescriptor typeDescriptor) {
    return typeDescriptor != null
        && !typeDescriptor.isNative()
        && !typeDescriptor.isJsFunctionInterface();
  }

  /** Returns the field descriptor for a the field backing the static setter/getters. */
  private static FieldDescriptor getBackingFieldDescriptor(FieldDescriptor fieldDescriptor) {
    checkArgument(
        fieldDescriptor.isStatic()
            && fieldDescriptor.getOrigin() != FieldOrigin.SYNTHETIC_BACKING_FIELD);
    return FieldDescriptor.Builder.from(fieldDescriptor)
        .setJsInfo(JsInfo.NONE)
        .setOrigin(FieldOrigin.SYNTHETIC_BACKING_FIELD)
        .build();
  }

  private static MethodDescriptor getSetterMethodDescriptor(FieldDescriptor fieldDescriptor) {
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(fieldDescriptor.getEnclosingTypeDescriptor())
        .setName(fieldDescriptor.getName())
        .setOrigin(MethodOrigin.SYNTHETIC_PROPERTY_SETTER)
        .setParameterTypeDescriptors(fieldDescriptor.getTypeDescriptor())
        .setReturnTypeDescriptor(PrimitiveTypes.VOID)
        .setVisibility(fieldDescriptor.getVisibility())
        .setJsInfo(
            fieldDescriptor.isJsProperty()
                ? JsInfo.Builder.from(fieldDescriptor.getJsInfo())
                    .setJsMemberType(JsMemberType.SETTER)
                    .setJsName(fieldDescriptor.getName())
                    .build()
                : JsInfo.NONE)
        .setStatic(true)
        .build();
  }

  private static MethodDescriptor getGetterMethodDescriptor(FieldDescriptor fieldDescriptor) {
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(fieldDescriptor.getEnclosingTypeDescriptor())
        .setName(fieldDescriptor.getName())
        .setOrigin(MethodOrigin.SYNTHETIC_PROPERTY_GETTER)
        .setReturnTypeDescriptor(fieldDescriptor.getTypeDescriptor())
        .setVisibility(fieldDescriptor.getVisibility())
        .setJsInfo(
            fieldDescriptor.isJsProperty()
                ? JsInfo.Builder.from(fieldDescriptor.getJsInfo())
                    .setJsMemberType(JsMemberType.GETTER)
                    .setJsName(fieldDescriptor.getName())
                    .build()
                : JsInfo.NONE)
        .setStatic(true)
        .build();
  }
}
