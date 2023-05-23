/*
 * Copyright 2021 Google Inc.
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
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeLiteral;

/** Adds support for type literals and getClass(). */
public class ImplementClassMetadataViaGetters extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    synthesizeLazyGetClassGetter(type);
    synthesizeGetClassImplememntationsMethods(type);
    replaceTypeLiterals(type);
  }

  private void synthesizeLazyGetClassGetter(Type type) {
    if (type.isNative()) {
      return;
    }
    createClassLiteralMethod(type);
  }

  private static final String GET_CLASS_IMPL_METHOD_NAME = "$getClassImpl";

  /** Synthesizes the getClass() override for this class. */
  private static void synthesizeGetClassImplememntationsMethods(Type type) {
    if (type.isInterface() || type.isAbstract() || type.isNative()) {
      return;
    }

    if (TypeDescriptors.isJavaLangObject(type.getTypeDescriptor())) {
      // Remove $getClassImpl from object since it will be synthesized to return
      // Object.class.
      type.getMembers()
          .removeIf(m -> m.getDescriptor().getName().equals(GET_CLASS_IMPL_METHOD_NAME));
              }

    // return Type.class;
    type.addMember(
        Method.newBuilder()
            .setMethodDescriptor(getGetClassImplMethodDescriptor(type.getTypeDescriptor()))
            .addStatements(
                ReturnStatement.newBuilder()
                    .setExpression(
                        getTypeLiteral(type.getSourcePosition(), type.getTypeDescriptor()))
                    .setSourcePosition(SourcePosition.NONE)
                    .build())
            .setSourcePosition(SourcePosition.NONE)
            .build());
  }

  private static MethodDescriptor getGetClassImplMethodDescriptor(
      DeclaredTypeDescriptor typeDescriptor) {
    return MethodDescriptor.Builder.from(
            TypeDescriptors.get().javaLangObject.getMethodDescriptor(GET_CLASS_IMPL_METHOD_NAME))
        .setEnclosingTypeDescriptor(typeDescriptor)
        .setSynthetic(true)
        .build();
  }

  private static Expression getTypeLiteral(
      SourcePosition sourcePosition, DeclaredTypeDescriptor typeDescriptor) {
    if (TypeDescriptors.isWasmArraySubtype(typeDescriptor)) {
      // This is a WasmArray implementation class, hence we need to return the type literal for
      // the corresponding array type.
      // TODO(b/184675805): Remove or refactor workaround when full support for array class
      // metadata is implemented.
      return new TypeLiteral(
          sourcePosition, typeDescriptor.getFieldDescriptor("elements").getTypeDescriptor());
    }

    return new TypeLiteral(sourcePosition, typeDescriptor);
  }

  /** Replaces type literals in the AST for the corresponding call to the synthetic lazy getter. */
  private void replaceTypeLiterals(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteTypeLiteral(TypeLiteral typeLiteral) {
            return implementTypeLiteral(typeLiteral.getReferencedTypeDescriptor());
          }
        });
  }

  /** Returns a call to obtain the class literal using the corresponding lazy getter. */
  private Expression implementTypeLiteral(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isNative()) {
      // TODO(b/283369109): Decide the semantics of type literal for native types, for now
      // return a null.
      getProblems()
          .warning(
              "Native type '%s' class literals are not supported (b/283369109).",
              typeDescriptor.getReadableDescription());
      return TypeDescriptors.get().javaLangClass.getNullValue();
    }
    if (typeDescriptor.isArray()) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      return MethodCall.Builder.from(
              TypeDescriptors.get()
                  .javaLangClass
                  .getMethodDescriptor("getArrayType", PrimitiveTypes.INT))
          .setQualifier(implementTypeLiteral(arrayTypeDescriptor.getLeafTypeDescriptor()))
          .setArguments(NumberLiteral.fromInt(arrayTypeDescriptor.getDimensions()))
          .build();
    }
    return getClassLiteralMethodCall(typeDescriptor);
  }

  private Expression getClassLiteralMethodCall(TypeDescriptor typeDescriptor) {
    return MethodCall.Builder.from(getLazyClassMetadataGetterMethodDescriptor(typeDescriptor))
        .build();
  }

  /** Creates the lazy getter for the class literal of {@code type}. */
  private void createClassLiteralMethod(Type type) {
    DeclaredTypeDescriptor typeDescriptor = type.getTypeDescriptor();

    type.synthesizeLazilyInitializedField(
        "$class",
        getClassObjectCreationExpression(typeDescriptor),
        getLazyClassMetadataGetterMethodDescriptor(typeDescriptor));

    // If it is a boxed type generate also the getter for the corresponding primitive type literal.
    if (TypeDescriptors.isBoxedType(typeDescriptor)
        || TypeDescriptors.isJavaLangVoid(typeDescriptor)) {
      PrimitiveTypeDescriptor primitiveTypeDescriptor = typeDescriptor.toUnboxedType();
      type.synthesizeLazilyInitializedField(
          "$primitiveClass",
          getClassObjectCreationExpression(primitiveTypeDescriptor),
          getLazyClassMetadataGetterMethodDescriptor(primitiveTypeDescriptor));
    }
  }

  /** Creates expression that instantiates the class object for {@code typeDescriptor}. */
  private Expression getClassObjectCreationExpression(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isPrimitive()) {
      PrimitiveTypeDescriptor primitiveTypeDescriptor = (PrimitiveTypeDescriptor) typeDescriptor;
      return getClassObjectCreationExpression(
          "createForPrimitive",
          new StringLiteral(primitiveTypeDescriptor.getSimpleSourceName()),
          new StringLiteral(primitiveTypeDescriptor.getSignature()));
    }

    DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
    if (declaredTypeDescriptor.isClass()) {
      DeclaredTypeDescriptor superTypeDescriptor = declaredTypeDescriptor.getSuperTypeDescriptor();
      Expression superClassExpression =
          superTypeDescriptor == null
              ? TypeDescriptors.get().javaLangClass.getDefaultValue()
              : getClassLiteralMethodCall(superTypeDescriptor);
      return getClassObjectCreationExpression(
          "createForClass",
          new StringLiteral(declaredTypeDescriptor.getQualifiedBinaryName()),
          superClassExpression);
    } else if (declaredTypeDescriptor.isInterface()) {
      return getClassObjectCreationExpression(
          "createForInterface", new StringLiteral(declaredTypeDescriptor.getQualifiedBinaryName()));
    } else if (declaredTypeDescriptor.isEnum()) {
      return getClassObjectCreationExpression(
          "createForEnum", new StringLiteral(declaredTypeDescriptor.getQualifiedBinaryName()));
    } else {
      throw new IllegalStateException("Unexpected type: " + typeDescriptor);
    }
  }

  /** Creates expression that instantiates the class object for {@code typeDescriptor}. */
  private Expression getClassObjectCreationExpression(
      String creationMethodName, Expression... arguments) {
    return MethodCall.Builder.from(
            TypeDescriptors.get().javaLangClass.getMethodDescriptorByName(creationMethodName))
        .setArguments(arguments)
        .build();
  }

  /** Returns the descriptor for the getter of {@code typeDescriptor}. */
  private static MethodDescriptor getLazyClassMetadataGetterMethodDescriptor(
      TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      return getLazyClassMetadataGetterMethodDescriptor(
          (DeclaredTypeDescriptor) typeDescriptor, "$getClassMetadata");
    }
    checkState(typeDescriptor instanceof PrimitiveTypeDescriptor);
    return getLazyClassMetadataGetterMethodDescriptor(
        typeDescriptor.toBoxedType(), "$getClassMetadataForPrimitive");
  }
  /**
   * Returns the descriptor for the getter of the class metadata for {@code
   * enclosingTypeDescriptor}.
   */
  private static MethodDescriptor getLazyClassMetadataGetterMethodDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor, String name) {
    return MethodDescriptor.newBuilder()
        .setName(name)
        .setReturnTypeDescriptor(TypeDescriptors.get().javaLangClass)
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setOrigin(MethodOrigin.SYNTHETIC_CLASS_LITERAL_GETTER)
        .setStatic(true)
        .setSynthetic(true)
        .setSideEffectFree(true)
        .build();
  }
}
