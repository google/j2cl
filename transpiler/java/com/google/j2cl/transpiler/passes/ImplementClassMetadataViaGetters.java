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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Kind;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeLiteral;
import com.google.j2cl.transpiler.ast.Visibility;
import java.util.HashMap;
import java.util.Map;

/** Adds support for type literals and getClass(). */
public class ImplementClassMetadataViaGetters extends LibraryNormalizationPass {

  private final Type classLiteralPoolType =
      new Type(
          SourcePosition.NONE,
          Visibility.PRIVATE,
          TypeDeclaration.newBuilder()
              .setClassComponents(ImmutableList.of("javaemul.internal.ClassLiteralPool"))
              .setKind(Kind.INTERFACE)
              .build());

  @Override
  public void applyTo(Library library) {
    synthesizeGetClassMethods(library);
    replaceTypeLiterals(library);
    Iterables.getLast(library.getCompilationUnits()).addType(classLiteralPoolType);
  }

  /** Synthesizes the getClass() override for this class. */
  private static void synthesizeGetClassMethods(Library library) {
    library.getCompilationUnits().stream()
        .flatMap(cu -> cu.getTypes().stream())
        .forEach(
            t -> {
              if (TypeDescriptors.isJavaLangObject(t.getTypeDescriptor())) {
                // Remove getClass from object since it will be synthesized to return Object.class.
                t.getMembers().removeIf(m -> m.getDescriptor().getName().equals("getClass"));
              }

              // return Type.class;
              t.addMember(
                  Method.newBuilder()
                      .setMethodDescriptor(getGetClassMethodDescriptor(t.getTypeDescriptor()))
                      .addStatements(
                          ReturnStatement.newBuilder()
                              .setExpression(
                                  new TypeLiteral(t.getSourcePosition(), t.getTypeDescriptor()))
                              .setTypeDescriptor(specializeJavaLangClass(t.getTypeDescriptor()))
                              .setSourcePosition(SourcePosition.NONE)
                              .build())
                      .setSourcePosition(SourcePosition.NONE)
                      .build());
            });
  }

  private static MethodDescriptor getGetClassMethodDescriptor(
      DeclaredTypeDescriptor typeDescriptor) {
    return MethodDescriptor.Builder.from(
            TypeDescriptors.get().javaLangObject.getMethodDescriptor("getClass"))
        .setEnclosingTypeDescriptor(typeDescriptor)
        .setSynthetic(true)
        .build();
  }

  private static DeclaredTypeDescriptor specializeJavaLangClass(TypeDescriptor typeArgument) {
    if (typeArgument.isPrimitive()) {
      typeArgument = typeArgument.toBoxedType();
    }
    DeclaredTypeDescriptor javaLangClass = TypeDescriptors.get().javaLangClass;
    return (DeclaredTypeDescriptor)
        javaLangClass.specializeTypeVariables(
            ImmutableMap.of(
                javaLangClass.getTypeDeclaration().getTypeParameterDescriptors().get(0),
                typeArgument));
  }

  /** Replaces type literals in the AST for the corresponding call to the synthetic lazy getter. */
  private void replaceTypeLiterals(Library library) {
    library.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteTypeLiteral(TypeLiteral typeLiteral) {
            return implementTypeLiteral(typeLiteral.getReferencedTypeDescriptor());
          }
        });
  }

  /** Returns a call to obtain the class literal using the corresponding lazy getter. */
  private Expression implementTypeLiteral(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isArray()) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      return MethodCall.Builder.from(
              TypeDescriptors.get()
                  .javaLangClass
                  .getMethodDescriptor("getArrayType", PrimitiveTypes.INT))
          .setQualifier(getClassLiteralMethodCall(arrayTypeDescriptor.getLeafTypeDescriptor()))
          .setArguments(NumberLiteral.fromInt(arrayTypeDescriptor.getDimensions()))
          .build();
    }
    return getClassLiteralMethodCall(typeDescriptor);
  }

  private Expression getClassLiteralMethodCall(TypeDescriptor typeDescriptor) {
    return MethodCall.Builder.from(getOrCreateClassLiteralMethod(typeDescriptor)).build();
  }

  private final Map<TypeDescriptor, MethodDescriptor> lazyGettersByType = new HashMap<>();

  /**
   * Returns the descriptor for the getter of {@code typeDescriptor}, creating it if it did not
   * exist.
   */
  private MethodDescriptor getOrCreateClassLiteralMethod(TypeDescriptor typeDescriptor) {
    typeDescriptor = typeDescriptor.toUnparameterizedTypeDescriptor();
    MethodDescriptor getClassLiteralMethod = lazyGettersByType.get(typeDescriptor);
    if (getClassLiteralMethod == null) {
      String fieldName =
          typeDescriptor.isPrimitive()
              ? ((PrimitiveTypeDescriptor) typeDescriptor).getSimpleSourceName()
              : ((DeclaredTypeDescriptor) typeDescriptor).getQualifiedSourceName();

      getClassLiteralMethod =
          classLiteralPoolType.synthesizeLazilyInitializedField(
              fieldName, getClassObjectCreationExpression(typeDescriptor));

      lazyGettersByType.put(typeDescriptor, getClassLiteralMethod);
    }

    return getClassLiteralMethod;
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
}
