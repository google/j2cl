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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.Visibility;

/**
 * This class generates the AST structure for the synthesized static methods values and valueOf on
 * Enum types. Additionally, we add a private static field "namesToValuesMap" which is created the
 * first time valueOf() is called and allows for quick lookup of Enum values by (String) name.
 */
public class AddEnumImplicitMethods extends NormalizationPass {
  private static final String VALUES_METHOD_NAME = "values";
  private static final String NAMES_TO_VALUES_MAP_FIELD_NAME = "namesToValuesMap";
  private static final String CREATE_MAP_METHOD_NAME = "createMapFromValues";

  @Override
  public void applyTo(Type enumType) {
    if (enumType.isJsEnum() || !enumType.isEnum()) {
      // JsEnums do not support values() nor valueOf().
      return;
    }
    checkArgument(enumType.isEnum());
    createValueOfMethod(enumType);
    createValuesMethod(enumType);
  }

  /**
   * Creates the ast needed for valueOf(String name) which is of the form: <code>
   * private Object namesToValuesMap = null;
   * public static EnumType valueOf(String name) {
   *   if(namesToValuesMap == null){
   *     namesToValuesMap = Enums.createMapFromValues(this.values());
   *   }
   *   return Enums.getValueFromNameAndMap(name, namesToValuesMap);
   * }
   * </code>
   */
  private void createValueOfMethod(Type enumType) {
    SourcePosition sourcePosition = enumType.getSourcePosition();
    DeclaredTypeDescriptor typeDescriptor = enumType.getTypeDescriptor();

    // Create a field to hold the names to values map.
    FieldDescriptor namesToValuesMapFieldDescriptor =
        getNamesToValuesMapFieldDescriptor(typeDescriptor);
    enumType.addMember(
        Field.Builder.from(namesToValuesMapFieldDescriptor)
            .setSourcePosition(enumType.getSourcePosition())
            .build());

    Variable nameParameter =
        Variable.newBuilder()
            .setName("name")
            .setTypeDescriptor(TypeDescriptors.get().javaLangString.toNonNullable())
            .setParameter(true)
            .build();

    // if (namesToValuesMap == null)
    Statement ifStatement =
        IfStatement.newBuilder()
            .setSourcePosition(sourcePosition)
            .setConditionExpression(
                FieldAccess.Builder.from(namesToValuesMapFieldDescriptor).build().infixEqualsNull())
            .setThenStatement(
                Block.newBuilder()
                    .setSourcePosition(sourcePosition)
                    //   namesToValuesMap = createMapFromValues(this.values());
                    .setStatements(
                        BinaryExpression.Builder.asAssignmentTo(namesToValuesMapFieldDescriptor)
                            .setRightOperand(
                                RuntimeMethods.createEnumsCreateMapFromValuesMethodCall(
                                    MethodCall.Builder.from(
                                            typeDescriptor.getMethodDescriptor(VALUES_METHOD_NAME))
                                        .build()))
                            .build()
                            .makeStatement(sourcePosition))
                    .build())
            .build();

    // return getValueFromNameAndMap(name, namesToValuesMap);
    Statement returnStatement =
        ReturnStatement.newBuilder()
            .setExpression(
                RuntimeMethods.createEnumsGetValueFromNameAndMapMethodCall(
                    typeDescriptor,
                    nameParameter.createReference(),
                    FieldAccess.Builder.from(namesToValuesMapFieldDescriptor).build()))
            .setSourcePosition(sourcePosition)
            .build();

    enumType.addMember(
        Method.newBuilder()
            .setMethodDescriptor(
                typeDescriptor.getMethodDescriptor(
                    MethodDescriptor.VALUE_OF_METHOD_NAME, TypeDescriptors.get().javaLangString))
            .setParameters(nameParameter)
            .addStatements(ifStatement, returnStatement)
            .setSourcePosition(sourcePosition)
            .build());
  }

  private FieldDescriptor getNamesToValuesMapFieldDescriptor(
      DeclaredTypeDescriptor enumTypeDescriptor) {
    return FieldDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enumTypeDescriptor)
        .setName(NAMES_TO_VALUES_MAP_FIELD_NAME)
        .setTypeDescriptor(getEnumMapTypeDescriptor(enumTypeDescriptor.toNonNullable()))
        .setStatic(true)
        .setVisibility(Visibility.PRIVATE)
        .build();
  }

  protected TypeDescriptor getEnumMapTypeDescriptor(DeclaredTypeDescriptor enumTypeDescriptor) {
    MethodDescriptor createMapMethodDescriptor =
        TypeDescriptors.get()
            .javaemulInternalEnums
            .getMethodDescriptorByName(CREATE_MAP_METHOD_NAME);
    // There should be 1 type variable. It should be specialized to the enum type.
    TypeVariable enumTypeVariable =
        createMapMethodDescriptor.getTypeParameterTypeDescriptors().get(0);
    return createMapMethodDescriptor
        .getReturnTypeDescriptor()
        .specializeTypeVariables(
            ImmutableMap.of(enumTypeVariable, enumTypeDescriptor.toNonNullable()));
  }

  /**
   * Creates the ast needed for values() which is of the form: <code>
   * static EnumType[] values() {
   *   return [
   *     EnumType.VALUE1,
   *     EnumType.VALUE2 ...
   *   ];
   * }
   * </code>
   */
  private static void createValuesMethod(Type enumType) {
    SourcePosition sourcePosition = enumType.getSourcePosition();

    // Create method body.
    ImmutableList<Expression> values =
        enumType.getEnumFields().stream()
            .map(enumField -> FieldAccess.Builder.from(enumField.getDescriptor()).build())
            .collect(toImmutableList());

    ArrayTypeDescriptor arrayTypeDescriptor =
        ArrayTypeDescriptor.newBuilder()
            .setComponentTypeDescriptor(enumType.getTypeDescriptor().toNonNullable())
            .build();

    enumType.addMember(
        Method.newBuilder()
            .setMethodDescriptor(
                enumType.getTypeDescriptor().getMethodDescriptor(VALUES_METHOD_NAME))
            .addStatements(
                ReturnStatement.newBuilder()
                    .setExpression(new ArrayLiteral(arrayTypeDescriptor, values))
                    .setSourcePosition(sourcePosition)
                    .build())
            .setSourcePosition(sourcePosition)
            .build());
  }
}
