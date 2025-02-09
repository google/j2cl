/*
 * Copyright 2025 Google Inc.
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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RecordConstructor;
import com.google.j2cl.transpiler.ast.RecordField;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.Visibility;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.j2cl.transpiler.ast.RuntimeMethods.createGetClassMethodCall;
import static com.google.j2cl.transpiler.ast.RuntimeMethods.createObjectsHashMethodCall;

public class EnrichRecord extends NormalizationPass {

    @Override
    public void applyTo(Type type) {

        if (type.isRecord()) {
            processRecord(type);
        }
    }

    private void processRecord(Type type) {
        Map<String, Field> fields = type.getMembers().stream()
                .filter(f -> f instanceof RecordField)
                .map(f -> ((RecordField) f).getField())
                .collect(Collectors.toMap(Field::getSimpleJsName, Function.identity(), (o1, o2) -> o1, HashMap::new));

        Method constructor = type.getMembers().stream()
                .filter(m -> m instanceof RecordConstructor)
                .map(m -> ((RecordConstructor) m).getMethod())
                .findFirst()
                .orElseThrow();

        addAssignmentStatements(constructor, fields);
        addFields(type, fields);
        addGetters(type, constructor, fields);
        addHashCode(type, fields);
        addEquals(type, fields);
        addToString(type, fields);

        type.addMember(constructor);
    }

    private void addGetters(Type type, Method constructor, Map<String, Field> fields) {
        for (Variable parameter : constructor.getParameters()) {
            String argName = parameter.getName();
            Field field = fields.get(argName);
            type.addMember(Method.newBuilder()
                    .setSourcePosition(SourcePosition.NONE)
                    .setMethodDescriptor(MethodDescriptor.newBuilder()
                            .setEnclosingTypeDescriptor(type.getTypeDescriptor())
                            .setVisibility(Visibility.PUBLIC)
                            .setName(field.getDescriptor().getName())
                            .setReturnTypeDescriptor(field.getDescriptor().getTypeDescriptor())
                            .build())
                    .addStatements(
                            AstUtils.createReturnOrExpressionStatement(
                                    SourcePosition.NONE,
                                    FieldAccess.newBuilder()
                                            .setQualifier(new ThisReference(type.getTypeDescriptor()))
                                            .setSourcePosition(SourcePosition.NONE)
                                            .setTarget(field.getDescriptor())
                                            .build(),
                                    field.getDescriptor().getTypeDescriptor()))
                    .build());
        }
    }

    private void addFields(Type type, Map<String, Field> fields) {
        for (Field field : fields.values()) {
            type.addMember(field);
        }
    }

    private void addAssignmentStatements(Method constructor, Map<String, Field> fields) {
        for (Variable parameter : constructor.getParameters()) {
            String argName = parameter.getName();
            Field field = fields.get(argName);

            constructor.getBody().getStatements().add(BinaryExpression.Builder
                    .asAssignmentTo(field.getDescriptor())
                    .setRightOperand(parameter)
                    .build()
                    .makeStatement(SourcePosition.NONE));
        }
    }

    private void addHashCode(Type type, Map<String, Field> fields) {
        List<String> sortedFields = fields.keySet().stream().sorted().collect(Collectors.toList());
        Method.Builder builder = Method.newBuilder()
                .setSourcePosition(SourcePosition.NONE)
                .setMethodDescriptor(MethodDescriptor.newBuilder()
                        .setEnclosingTypeDescriptor(type.getTypeDescriptor())
                        .setVisibility(Visibility.PUBLIC)
                        .setName("hashCode")
                        .setReturnTypeDescriptor(PrimitiveTypes.INT)
                        .build());

        ArrayLiteral.Builder arrayLiteralBuilder =
                ArrayLiteral.newBuilder()
                        .setTypeDescriptor(ArrayTypeDescriptor.newBuilder()
                                .setComponentTypeDescriptor(TypeDescriptors.get().javaLangObject)
                                .build());

        for (String fieldName : sortedFields) {
            Field field = fields.get(fieldName);

            TypeDescriptor typeDescriptor = field.getDescriptor().getTypeDescriptor();
            FieldAccess fieldAccess = FieldAccess.newBuilder()
                    .setQualifier(new ThisReference(type.getTypeDescriptor()))
                    .setSourcePosition(SourcePosition.NONE)
                    .setTarget(field.getDescriptor())
                    .build();

            if (typeDescriptor.isPrimitive()) {
                DeclaredTypeDescriptor boxed = typeDescriptor.toBoxedType();
                arrayLiteralBuilder.addValueExpressions(MethodCall.Builder.from(MethodDescriptor.newBuilder()
                                .setName(MethodDescriptor.VALUE_OF_METHOD_NAME)
                                .setEnclosingTypeDescriptor(boxed)
                                .setStatic(true)
                                .setReturnTypeDescriptor(boxed)
                                .setParameterDescriptors(MethodDescriptor.ParameterDescriptor.newBuilder()
                                        .setTypeDescriptor(typeDescriptor)
                                        .build())
                                .build())
                        .setArguments(fieldAccess)
                        .build());
            } else {
                arrayLiteralBuilder.addValueExpressions(fieldAccess);
            }
        }

        type.addMember(builder.setBody(Block.newBuilder()
                .addStatement(AstUtils.createReturnOrExpressionStatement(
                        SourcePosition.NONE,
                        createObjectsHashMethodCall(arrayLiteralBuilder.build()),
                        PrimitiveTypes.INT))
                .build()).build());
    }

    private void addEquals(Type type, Map<String, Field> fields) {
        Variable param = Variable.newBuilder()
                .setParameter(true)
                .setName("other")
                .setTypeDescriptor(TypeDescriptors.get().javaLangObject)
                .build();

        Variable other = Variable.newBuilder()
                .setTypeDescriptor(type.getTypeDescriptor())
                .setName(type.getTypeDescriptor().getSimpleSourceName().toLowerCase(Locale.ROOT))
                .setFinal(false)
                .setParameter(false)
                .setSourcePosition(SourcePosition.NONE)
                .build();

        Method.Builder builder = Method.newBuilder()
                .setSourcePosition(SourcePosition.NONE)
                .setMethodDescriptor(MethodDescriptor.newBuilder()
                        .setEnclosingTypeDescriptor(type.getTypeDescriptor())
                        .setVisibility(Visibility.PUBLIC)
                        .setName("equals")
                        .setReturnTypeDescriptor(PrimitiveTypes.BOOLEAN)
                        .setParameterDescriptors(MethodDescriptor.ParameterDescriptor.newBuilder()
                                .setTypeDescriptor(TypeDescriptors.get().javaLangObject)
                                .build())
                        .build())
                .setParameters(param)
                .setBody(Block.newBuilder()
                        .addStatement(IfStatement.newBuilder()
                                .setSourcePosition(SourcePosition.NONE)
                                .setConditionExpression(new ThisReference(type.getTypeDescriptor()).infixEquals(param.createReference()))
                                .setThenStatement(AstUtils.createReturnOrExpressionStatement(
                                        SourcePosition.NONE,
                                        BooleanLiteral.get(true),
                                        PrimitiveTypes.BOOLEAN))
                                .build())
                        .addStatement(IfStatement.newBuilder()
                                .setSourcePosition(SourcePosition.NONE)
                                .setConditionExpression(param.createReference()
                                        .infixEqualsNull()
                                        .infixOr(createGetClassMethodCall(new ThisReference(type.getTypeDescriptor())).infixNotEquals(
                                                createGetClassMethodCall(param.createReference())
                                        )))
                                .setThenStatement(AstUtils.createReturnOrExpressionStatement(
                                        SourcePosition.NONE,
                                        BooleanLiteral.get(false),
                                        PrimitiveTypes.BOOLEAN))
                                .build())
                        .addStatement(VariableDeclarationExpression.newBuilder()
                                .addVariableDeclaration(other,
                                        CastExpression.newBuilder()
                                                .setCastTypeDescriptor(type.getTypeDescriptor())
                                                .setExpression(param.createReference())
                                                .build()
                                )
                                .build().makeStatement(SourcePosition.NONE))
                        .addStatement(ReturnStatement.newBuilder()
                                .setExpression(addChecks(other, fields.values()))
                                .setSourcePosition(SourcePosition.NONE).build())
                        .build());
        type.addMember(builder.build());
    }

    private void addToString(Type type, Map<String, Field> fields) {
        String beanName = type.getTypeDescriptor().getSimpleSourceName();
        TypeDeclaration sbType = TypeDeclaration.newBuilder()
                .setQualifiedSourceName(StringBuffer.class.getCanonicalName())
                .setKind(TypeDeclaration.Kind.CLASS)
                .build();

        Variable sbVariable = Variable.newBuilder()
                .setTypeDescriptor(sbType.toDescriptor())
                .setName("sb")
                .setFinal(false)
                .setParameter(false)
                .setSourcePosition(SourcePosition.NONE)
                .build();

        Block.Builder block = Block.newBuilder();

        Expression sbInstance = NewInstance.Builder.from(MethodDescriptor.newBuilder()
                        .setConstructor(true)
                        .setEnclosingTypeDescriptor(sbType.toDescriptor())
                        .setReturnTypeDescriptor(sbType.toDescriptor())
                        .build())
                .build();

        MethodDescriptor appendMethodDescriptor = MethodDescriptor.newBuilder()
                        .setConstructor(false)
                        .setEnclosingTypeDescriptor(sbType.toDescriptor())
                        .setName("append")
                        .setParameterDescriptors(MethodDescriptor.ParameterDescriptor
                                .newBuilder()
                                .setTypeDescriptor(TypeDescriptors.get().javaLangObject)
                                .build())
                        .setReturnTypeDescriptor(sbType.toDescriptor()).build();

        sbInstance = MethodCall.Builder.from(appendMethodDescriptor)
                        .setQualifier(sbInstance)
                        .setArguments(new StringLiteral(beanName +"["))
                        .build();
        List<String> fieldNames = fields.keySet().stream().sorted().collect(Collectors.toList());
        for(int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            Field field = fields.get(fieldName);
            FieldAccess.Builder.from(field).setDefaultInstanceQualifier().build();

            sbInstance = MethodCall.Builder.from(appendMethodDescriptor)
                    .setQualifier(sbInstance)
                    .setArguments(new StringLiteral((i == 0 ? "" : ", ") + fieldName + "="))
                    .build();

            sbInstance = MethodCall.Builder.from(appendMethodDescriptor)
                    .setQualifier(sbInstance)
                    .setArguments(FieldAccess.Builder.from(field).setDefaultInstanceQualifier().build())
                    .build();
        }

        sbInstance = MethodCall.Builder.from(appendMethodDescriptor)
                .setQualifier(sbInstance)
                .setArguments(new StringLiteral("]"))
                .build();

        block.addStatement(VariableDeclarationExpression.newBuilder()
                .addVariableDeclaration(sbVariable, sbInstance)
                .build().makeStatement(SourcePosition.NONE));


        block.addStatement(ReturnStatement.newBuilder()
                .setSourcePosition(SourcePosition.NONE)
                .setExpression(MethodCall.Builder.from(MethodCall.Builder.from(MethodDescriptor.newBuilder()
                                        .setConstructor(false)
                                        .setEnclosingTypeDescriptor(sbType.toDescriptor())
                                        .setName("toString")
                                        .setReturnTypeDescriptor(TypeDescriptors.get().javaLangString)
                                        .build())
                                .setQualifier(sbVariable.createReference())
                                .build())
                        .build())
                .build());

        Method.Builder builder = Method.newBuilder()
                .setSourcePosition(SourcePosition.NONE)
                .setMethodDescriptor(MethodDescriptor.newBuilder()
                        .setEnclosingTypeDescriptor(type.getTypeDescriptor())
                        .setVisibility(Visibility.PUBLIC)
                        .setName("toString")
                        .setReturnTypeDescriptor(TypeDescriptors.get().javaLangString)
                        .build());

        type.addMember(builder.setBody(block.build()).build());
    }

    private Expression addChecks(Variable other, Collection<Field> fields) {
        MultiExpression.Builder builder = MultiExpression.newBuilder();

        if (fields.isEmpty()) {
            return builder.addExpressions(BooleanLiteral.get(true)).build();
        }

        if (fields.size() == 1) {
            Field field = fields.iterator().next();
            return builder.addExpressions(FieldAccess.Builder.from(field)
                            .setDefaultInstanceQualifier()
                            .build()
                            .infixEquals(FieldAccess.Builder.from(field)
                                    .setQualifier(other.createReference())
                                    .build()))
                    .build();
        }

        Stack<Expression> stack = new Stack<>();
        for (Field field : fields) {
            Expression expression = getFieldEqualsExpression(field, other);
            stack.push(expression);
        }

        Expression latest = stack.pop();
        while (!stack.isEmpty()) {
            Expression current = stack.pop();
            latest = BinaryExpression.newBuilder()
                    .setLeftOperand(current)
                    .setRightOperand(latest)
                    .setOperator(BinaryOperator.CONDITIONAL_AND).build();
        }

        builder.addExpressions(latest);
        return builder.build();
    }

    private Expression getFieldEqualsExpression(Field field, Variable other) {
        if (field.getDescriptor().getTypeDescriptor().isPrimitive()) {
            return FieldAccess.Builder.from(field).setDefaultInstanceQualifier().build()
                    .infixEquals(FieldAccess.Builder.from(field).setQualifier(other.createReference()).build());
        }
        return MethodCall.Builder.from(MethodDescriptor.newBuilder().setName("equals")
                        .setEnclosingTypeDescriptor((DeclaredTypeDescriptor) field.getDescriptor().getTypeDescriptor())
                        .setParameterDescriptors(MethodDescriptor.ParameterDescriptor
                                .newBuilder()
                                .setTypeDescriptor(TypeDescriptors.get().javaLangObject)
                                .build())
                        .setReturnTypeDescriptor(PrimitiveTypes.BOOLEAN)
                        .setVisibility(Visibility.PUBLIC)
                        .setStatic(false)
                        .build())
                .setQualifier(FieldAccess.Builder.from(field).setDefaultInstanceQualifier().build())
                .setArguments(FieldAccess.Builder.from(field)
                        .setQualifier(other.createReference())
                        .build())
                .build();
    }

}
