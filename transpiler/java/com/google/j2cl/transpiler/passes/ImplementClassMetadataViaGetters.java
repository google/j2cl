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

import static com.google.common.base.Predicates.not;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
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
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
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
    // Add the ClassLiteralPool type at the beginning of the library so that it does not
    // accidentally inherit an unrelated source position.
    // In the wasm output, code with no source position will inherit the last seen source position.
    library.getCompilationUnits().get(0).addType(/* position= */ 0, classLiteralPoolType);
  }

  /** Synthesizes the getClass() override for this class. */
  private static void synthesizeGetClassMethods(Library library) {
    library
        .streamTypes()
        .filter(not(Type::isInterface))
        .filter(not(Type::isAbstract))
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
                                  getTypeLiteral(t.getSourcePosition(), t.getTypeDescriptor()))
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
    return MethodCall.Builder.from(getOrCreateClassLiteralMethod(typeDescriptor))
        .build();
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
