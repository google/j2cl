package com.google.j2cl.frontend;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.JsUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.Visibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class generates the AST structure for the synthesized static methods values and valueOf on
 * Enum types. Additionally, we add a private static field "namesToValuesMap" which is created the
 * first time valueOf() is called and allows for quick lookup of Enum values by (String) name.
 */
public class EnumMethodsCreator {
  private static final String VALUE_OF_METHOD_NAME = "valueOf";
  private static final String VALUES_METHOD_NAME = "values";
  private static final String NAMES_TO_VALUES_MAP_FIELD_NAME = "namesToValuesMap";
  private static final String CREATE_MAP_METHOD_NAME = "createMapFromValues";
  private static final String GET_VALUE_METHOD_NAME = "getValueFromNameAndMap";

  private JavaType enumType;
  private FieldDescriptor namesToValuesMapFieldDescriptor;
  private MethodDescriptor valuesMethodDescriptor;
  private MethodDescriptor valueOfMethodDescriptor;

  public static void applyTo(JavaType enumType) {
    Preconditions.checkArgument(enumType.isEnum());
    EnumMethodsCreator instance = new EnumMethodsCreator(enumType);
    instance.run();
  }

  private EnumMethodsCreator(JavaType enumType) {
    boolean jsType = enumType.getDescriptor().isJsType();

    this.enumType = enumType;
    this.namesToValuesMapFieldDescriptor =
        FieldDescriptor.Builder.fromDefault(
                enumType.getDescriptor(),
                NAMES_TO_VALUES_MAP_FIELD_NAME,
                TypeDescriptors.createNative(
                    Collections.emptyList(),
                    // Import alias.
                    Lists.newArrayList("NativeObject"),
                    // Type parameters.
                    Lists.newArrayList(
                        TypeDescriptors.get().javaLangString,
                        TypeDescriptors.toNonNullable(enumType.getDescriptor())),
                    // Browser global
                    JsUtils.JS_GLOBAL,
                    // Native type name
                    "Object"))
            .setIsStatic(true)
            .setVisibility(Visibility.PRIVATE)
            .build();
    this.valuesMethodDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setIsStatic(true)
            .setEnclosingClassTypeDescriptor(enumType.getDescriptor())
            .setMethodName(VALUES_METHOD_NAME)
            .setReturnTypeDescriptor(TypeDescriptors.getForArray(enumType.getDescriptor(), 1))
            .setParameterTypeDescriptors(Arrays.asList(new TypeDescriptor[0]))
            .setJsInfo(jsType ? JsInfo.RAW : JsInfo.NONE)
            .build();
    this.valueOfMethodDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setIsStatic(true)
            .setEnclosingClassTypeDescriptor(enumType.getDescriptor())
            .setMethodName(VALUE_OF_METHOD_NAME)
            .setReturnTypeDescriptor(enumType.getDescriptor())
            .setParameterTypeDescriptors(TypeDescriptors.get().javaLangString)
            .setJsInfo(jsType ? JsInfo.RAW : JsInfo.NONE)
            .build();
  }

  private void run() {
    enumType.addField(
        Field.Builder.fromDefault(this.namesToValuesMapFieldDescriptor)
            .setInitializer(NullLiteral.NULL)
            .setPosition(-1) /* Position is not important */
            .build());
    enumType.addMethod(createValueOfMethod());
    enumType.addMethod(createValuesMethod());
  }

  /**
   * Creates the ast needed for valueOf(String name) which is of the form:
   *
   * <code>
   * private Object namesToValuesMap = null;
   * public static EnumType valueOf(String name) {
   *   if(namesToValuesMap == null){
   *     namesToValuesMap = Enums.createMapFromValues(this.values());
   *   }
   *   return Enums.getValueFromNameAndMap(name, namesToValuesMap);
   * }
   * </code>
   */
  private Method createValueOfMethod() {
    Variable nameParameter =
        new Variable("name", TypeDescriptors.get().javaLangString, false, true);
    MethodDescriptor createMapMethodDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setJsInfo(JsInfo.RAW)
            .setIsStatic(true)
            .setEnclosingClassTypeDescriptor(BootstrapType.ENUMS.getDescriptor())
            .setMethodName(CREATE_MAP_METHOD_NAME)
            .setReturnTypeDescriptor(namesToValuesMapFieldDescriptor.getTypeDescriptor())
            .setParameterTypeDescriptors(Arrays.asList(enumType.getDescriptor()))
            .build();
    MethodDescriptor getMethodDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setJsInfo(JsInfo.RAW)
            .setIsStatic(true)
            .setEnclosingClassTypeDescriptor(BootstrapType.ENUMS.getDescriptor())
            .setMethodName(GET_VALUE_METHOD_NAME)
            .setReturnTypeDescriptor(enumType.getDescriptor())
            .setParameterTypeDescriptors(
                Arrays.asList(
                    nameParameter.getTypeDescriptor(),
                    namesToValuesMapFieldDescriptor.getTypeDescriptor()))
            .build();

    Expression nameParameterAccess = nameParameter.getReference();
    Expression namesToValuesMapFieldAccess =
        new FieldAccess(null, this.namesToValuesMapFieldDescriptor);

    // If statement
    Expression namesToValuesMapIsNullComparison =
        new BinaryExpression(
            TypeDescriptors.get().primitiveBoolean,
            namesToValuesMapFieldAccess,
            BinaryOperator.EQUALS,
            NullLiteral.NULL);
    Expression valuesCall = MethodCall.createMethodCall(null, valuesMethodDescriptor);

    Expression createMapCall =
        MethodCall.createMethodCall(null, createMapMethodDescriptor, valuesCall);
    Expression assignMapCallToField =
        BinaryExpression.Builder.asAssignmentTo(namesToValuesMapFieldAccess)
            .setRightOperand(createMapCall)
            .build();
    Statement thenStatement = new ExpressionStatement(assignMapCallToField);
    Block thenBlock = new Block(thenStatement);
    Statement ifStatement = new IfStatement(namesToValuesMapIsNullComparison, thenBlock, null);

    // Return statement
    Expression getMethodCall =
        MethodCall.createMethodCall(
            null, getMethodDescriptor, nameParameterAccess, namesToValuesMapFieldAccess);
    Statement returnStatement =
        new ReturnStatement(
            getMethodCall, TypeDescriptors.getForArray(enumType.getDescriptor(), 1));

    return Method.Builder.fromDefault()
        .setMethodDescriptor(valueOfMethodDescriptor)
        .setParameters(nameParameter)
        .addStatements(ifStatement, returnStatement)
        .build();
  }

  /**
   * Creates the ast needed for values() which is of the form:
   *
   * <code>
   * static EnumType[] values() {
   *   return [
   *     EnumType.VALUE1,
   *     EnumType.VALUE2 ...
   *   ];
   * }
   * </code>
   */
  private Method createValuesMethod() {
    // Create method body.
    List<Expression> values = new ArrayList<>();
    for (Field enumField : enumType.getEnumFields()) {
      values.add(new FieldAccess(null, enumField.getDescriptor()));
    }
    Expression arrayOfValues =
        new ArrayLiteral(TypeDescriptors.getForArray(enumType.getDescriptor(), 1), values);
    Statement returnStatement =
        new ReturnStatement(
            arrayOfValues, TypeDescriptors.getForArray(enumType.getDescriptor(), 1));
    List<Statement> blockStatements = new ArrayList<>();
    blockStatements.add(returnStatement);

    return Method.Builder.fromDefault()
        .setMethodDescriptor(valuesMethodDescriptor)
        .addStatements(blockStatements)
        .build();
  }
}
