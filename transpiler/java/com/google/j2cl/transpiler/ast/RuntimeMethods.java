/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.transpiler.ast.TypeDescriptors.BootstrapType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/** Utilities to create method calls to the runtime. */
public class RuntimeMethods {

  /** Create a call to an Arrays method. */
  public static MethodCall createArraysMethodCall(String methodName, Expression... arguments) {
    return createArraysMethodCall(methodName, Arrays.asList(arguments));
  }

  /** Create a call to an Arrays method. */
  public static MethodCall createArraysMethodCall(String methodName, List<Expression> arguments) {
    return createRuntimeMethodCall(BootstrapType.ARRAYS.getDescriptor(), methodName, arguments);
  }

  /** Create a call to an array set. */
  public static Expression createArraySetMethodCall(
      Expression array, Expression index, Expression value) {

    // Get the type of the elements in the array.
    TypeDescriptor elementType =
        ((ArrayTypeDescriptor) array.getTypeDescriptor()).getComponentTypeDescriptor();

    // Create and return the method descriptor.
    return MethodCall.Builder.from(
            MethodDescriptor.newBuilder()
                .setJsInfo(JsInfo.RAW)
                .setStatic(true)
                .setEnclosingTypeDescriptor(BootstrapType.ARRAYS.getDescriptor())
                .setName("$set")
                .setParameterTypeDescriptors(
                    TypeDescriptors.get().javaLangObjectArray, // array
                    PrimitiveTypes.INT, // index
                    elementType)
                .setReturnTypeDescriptor(elementType)
                .build())
        .setArguments(array, index, value)
        .build();
  }

  /** Create a call to javaemul.internal.WasmArrayHelper.createArray method. */
  public static MethodCall createCreateMultiDimensionalArrayCall(
      Expression dimensions, Expression leafType) {
    return MethodCall.Builder.from(
            TypeDescriptors.get()
                .javaemulInternalWasmArray
                .getMethodDescriptor(
                    "createMultiDimensional",
                    ArrayTypeDescriptor.newBuilder()
                        .setComponentTypeDescriptor(PrimitiveTypes.INT)
                        .build(),
                    PrimitiveTypes.INT))
        .setArguments(dimensions, leafType)
        .build();
  }

  /** Create a call to an Class method. */
  public static MethodCall createClassGetMethodCall(Expression... arguments) {
    checkArgument(arguments.length == 1 || arguments.length == 2);

    List<TypeDescriptor> parameterTypeDescriptors =
        ImmutableList.of(TypeDescriptors.get().nativeFunction, PrimitiveTypes.INT);
    return MethodCall.Builder.from(
            MethodDescriptor.newBuilder()
                .setJsInfo(JsInfo.RAW)
                .setStatic(true)
                .setEnclosingTypeDescriptor(TypeDescriptors.get().javaLangClass)
                .setName("$get")
                // Allow the call to use the one or two parameter version.
                .setParameterTypeDescriptors(parameterTypeDescriptors.subList(0, arguments.length))
                .setReturnTypeDescriptor(TypeDescriptors.get().javaLangClass)
                .build())
        .setArguments(arguments)
        .build();
  }

  /** Create a call to Enums.createMapFromValues. */
  public static Expression createEnumsCreateMapFromValuesMethodCall(Expression values) {

    MethodDescriptor createMapMethodDescriptor =
        TypeDescriptors.get()
            .javaemulInternalEnums
            .getMethodDescriptorByName("createMapFromValues");

    // createMapFromValues is parameterized by T extends Enum, so specialize the method to the
    // right type.
    TypeVariable enumType = createMapMethodDescriptor.getTypeParameterTypeDescriptors().get(0);
    return MethodCall.Builder.from(
            createMapMethodDescriptor.specializeTypeVariables(
                ImmutableMap.of(
                    enumType,
                    ((ArrayTypeDescriptor) values.getTypeDescriptor())
                        .getComponentTypeDescriptor())))
        .setArguments(values)
        .build();
  }

  /** Create a call to Enums.getValueFromNameAndMap. */
  public static Expression createEnumsGetValueFromNameAndMapMethodCall(
      TypeDescriptor enumTypeDescriptor,
      Expression nameParameter,
      Expression namesToValuesMapParameter) {
    MethodDescriptor getValueMethodDescriptor =
        TypeDescriptors.get()
            .javaemulInternalEnums
            .getMethodDescriptorByName("getValueFromNameAndMap");

    // getValueFromNameAndMap is parameterized by T extends Enum, so specialize the method to the
    // right enum type.
    TypeVariable enumType = getValueMethodDescriptor.getTypeParameterTypeDescriptors().get(0);
    return MethodCall.Builder.from(
            getValueMethodDescriptor.specializeTypeVariables(
                ImmutableMap.of(enumType, enumTypeDescriptor)))
        .setArguments(nameParameter, namesToValuesMapParameter)
        .build();
  }

  /** Create a call to Enums.[boxingMethod] */
  public static Expression createEnumsBoxMethodCall(Expression value) {
    TypeDescriptor valueTypeDescriptor = value.getTypeDescriptor();
    String boxingMethodName =
        valueTypeDescriptor.getJsEnumInfo().supportsComparable() ? "boxComparable" : "box";

    MethodDescriptor boxingMethod =
        TypeDescriptors.get().javaemulInternalEnums.getMethodDescriptorByName(boxingMethodName);

    // boxing operations are parameterized by the JsEnum type, so specialize the method to the
    // right type.
    TypeVariable type = boxingMethod.getTypeParameterTypeDescriptors().get(0);
    return MethodCall.Builder.from(
            boxingMethod.specializeTypeVariables(ImmutableMap.of(type, valueTypeDescriptor)))
        .setArguments(value, valueTypeDescriptor.getMetadataConstructorReference())
        .build();
  }

  /** Create a call to Enums.unbox. */
  public static Expression createEnumsUnboxMethodCall(Expression expression) {
    return createEnumsMethodCall("unbox", expression);
  }

  public static Expression createEnumsMethodCall(String unbox, Expression... arguments) {
    MethodDescriptor methodDescriptor =
        TypeDescriptors.get().javaemulInternalEnums.getMethodDescriptorByName(unbox);

    return MethodCall.Builder.from(methodDescriptor).setArguments(arguments).build();
  }

  /** Create a call to an Equality method. */
  public static MethodCall createEqualityMethodCall(String methodName, Expression... arguments) {
    return createEqualityMethodCall(methodName, Arrays.asList(arguments));
  }

  /** Create a call to an Equality method. */
  public static MethodCall createEqualityMethodCall(String methodName, List<Expression> arguments) {
    return createRuntimeMethodCall(
        BootstrapType.NATIVE_EQUALITY.getDescriptor(), methodName, arguments);
  }

  /** Create a call to an Exceptions method. */
  public static MethodCall createExceptionsMethodCall(String methodName, Expression... arguments) {
    return MethodCall.Builder.from(
            TypeDescriptors.get().javaemulInternalExceptions.getMethodDescriptorByName(methodName))
        .setArguments(Arrays.asList(arguments))
        .build();
  }

  /** Create a call to InternalPreconditions.checkNotNull method. */
  public static MethodCall createCheckNotNullCall(Expression argument) {
    return MethodCall.Builder.from(
            TypeDescriptors.get()
                .javaemulInternalPreconditions
                .getMethodDescriptor("checkNotNull", TypeDescriptors.get().javaLangObject))
        .setArguments(argument)
        .build();
  }

  /** Create a call to an LongUtils method. */
  public static MethodCall createLongUtilsMethodCall(String methodName, Expression... arguments) {
    return createLongUtilsMethodCall(methodName, Arrays.asList(arguments));
  }

  /** Create a call to an LongUtils method. */
  public static MethodCall createLongUtilsMethodCall(
      String methodName, List<Expression> arguments) {
    return createRuntimeMethodCall(BootstrapType.LONG_UTILS.getDescriptor(), methodName, arguments);
  }

  /** Create a call to a LongUtils method. */
  public static Expression createLongUtilsMethodCall(
      String name,
      TypeDescriptor returnTypeDescriptor,
      Expression leftOperand,
      Expression rightOperand) {
    MethodDescriptor longUtilsMethodDescriptor =
        MethodDescriptor.newBuilder()
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setEnclosingTypeDescriptor(BootstrapType.LONG_UTILS.getDescriptor())
            .setName(name)
            .setParameterTypeDescriptors(PrimitiveTypes.LONG, PrimitiveTypes.LONG)
            .setReturnTypeDescriptor(returnTypeDescriptor)
            .build();
    // LongUtils.someOperation(leftOperand, rightOperand);
    return MethodCall.Builder.from(longUtilsMethodDescriptor)
        .setArguments(leftOperand, rightOperand)
        .build();
  }

  /** Create a call to a native Long method. */
  public static MethodCall createNativeLongMethodCall(String methodName, Expression... arguments) {
    return createNativeLongMethodCall(methodName, Arrays.asList(arguments));
  }

  /** Create a call to an native Long method. */
  private static MethodCall createNativeLongMethodCall(
      String methodName, List<Expression> arguments) {
    return createRuntimeMethodCall(
        BootstrapType.NATIVE_LONG.getDescriptor(), methodName, arguments);
  }

  public static Expression createMathImulMethodCall(
      Expression leftOperand, Expression rightOperand) {
    return MethodCall.Builder.from(
            MethodDescriptor.newBuilder()
                .setJsInfo(
                    JsInfo.newBuilder()
                        .setJsMemberType(JsMemberType.METHOD)
                        .setJsName("Math.imul")
                        .setJsNamespace(JsUtils.JS_PACKAGE_GLOBAL)
                        .build())
                .setName("imul")
                .setStatic(true)
                .setNative(true)
                .setEnclosingTypeDescriptor(TypeDescriptors.get().nativeObject)
                .setParameterTypeDescriptors(PrimitiveTypes.INT, PrimitiveTypes.INT)
                .setReturnTypeDescriptor(PrimitiveTypes.INT)
                .build())
        .setArguments(leftOperand, rightOperand)
        .build();
  }

  /** Create a call to a Primitives method. */
  public static MethodCall createPrimitivesMethodCall(String methodName, Expression argument) {
    MethodDescriptor narrowMethodDescriptor =
        TypeDescriptors.get().javaemulInternalPrimitives.getMethodDescriptorByName(methodName);

    return MethodCall.Builder.from(narrowMethodDescriptor).setArguments(argument).build();
  }

  /** Create a call to the corresponding narrowing Primitives method. */
  public static Expression createPrimitivesNarrowingMethodCall(
      Expression expression, PrimitiveTypeDescriptor toTypeDescriptor) {
    PrimitiveTypeDescriptor fromTypeDescriptor =
        (PrimitiveTypeDescriptor) expression.getTypeDescriptor();
    String methodName =
        String.format(
            "narrow%sTo%s",
            toProperCase(fromTypeDescriptor.getSimpleSourceName()),
            toProperCase(toTypeDescriptor.getSimpleSourceName()));

    return createPrimitivesMethodCall(methodName, expression);
  }

  /** Create a call to the corresponding widening Primitives method. */
  public static Expression createWideningPrimitivesMethodCall(
      Expression expression, PrimitiveTypeDescriptor toTypeDescriptor) {
    PrimitiveTypeDescriptor fromTypeDescriptor =
        (PrimitiveTypeDescriptor) expression.getTypeDescriptor();
    String methodName =
        String.format(
            "widen%sTo%s",
            toProperCase(fromTypeDescriptor.getSimpleSourceName()),
            toProperCase(toTypeDescriptor.getSimpleSourceName()));

    return createPrimitivesMethodCall(methodName, expression);
  }

  /** Return the String with first letter capitalized. */
  private static String toProperCase(String string) {
    if (string.isEmpty()) {
      return string;
    }
    return string.substring(0, 1).toUpperCase() + string.substring(1);
  }

  /** Create a call to a Util method. */
  public static MethodCall createUtilMethodCall(String methodName, Expression... arguments) {
    return createUtilMethodCall(methodName, Arrays.asList(arguments));
  }

  /** Create a call to an Util method. */
  public static MethodCall createUtilMethodCall(String methodName, List<Expression> arguments) {
    return createRuntimeMethodCall(
        BootstrapType.NATIVE_UTIL.getDescriptor(), methodName, arguments);
  }

  private static final ThreadLocal<Map<TypeDescriptor, Map<String, MethodInfo>>>
      runtimeMethodInfoByMethodNameByType =
          ThreadLocal.withInitial(
              () ->
                  ImmutableMap.<TypeDescriptor, Map<String, MethodInfo>>builder()
                      .put(
                          BootstrapType.ARRAYS.getDescriptor(),
                          // Arrays methods
                          ImmutableMap.<String, MethodInfo>builder()
                              .put(
                                  "$castTo",
                                  MethodInfo.newBuilder()
                                      .setReturnType(TypeDescriptors.get().javaLangObjectArray)
                                      .setParameters(
                                          TypeDescriptors.get().javaLangObject,
                                          TypeDescriptors.get().javaLangObject,
                                          PrimitiveTypes.INT)
                                      .build())
                              .put(
                                  "$castToNative",
                                  MethodInfo.newBuilder()
                                      .setReturnType(TypeDescriptors.get().javaLangObjectArray)
                                      .setParameters(TypeDescriptors.get().javaLangObject)
                                      .build())
                              .put(
                                  "$create",
                                  MethodInfo.newBuilder()
                                      .setReturnType(TypeDescriptors.get().javaLangObjectArray)
                                      .setParameters(
                                          TypeDescriptors.get().javaLangObjectArray,
                                          TypeDescriptors.get().javaLangObject)
                                      .build())
                              .put(
                                  "$createNative",
                                  MethodInfo.newBuilder()
                                      .setReturnType(TypeDescriptors.get().javaLangObjectArray)
                                      .setParameters(TypeDescriptors.get().javaLangObjectArray)
                                      .build())
                              .put(
                                  "$init",
                                  MethodInfo.newBuilder()
                                      .setReturnType(TypeDescriptors.get().javaLangObjectArray)
                                      .setParameters(
                                          TypeDescriptors.get().javaLangObjectArray,
                                          TypeDescriptors.get().javaLangObject,
                                          PrimitiveTypes.INT)
                                      .setRequiredParameters(2)
                                      .build())
                              .put(
                                  "$instanceIsOfType",
                                  MethodInfo.newBuilder()
                                      .setReturnType(TypeDescriptors.get().javaLangBoolean)
                                      .setParameters(
                                          TypeDescriptors.get().javaLangObject,
                                          TypeDescriptors.get().javaLangObject,
                                          PrimitiveTypes.INT)
                                      .build())
                              .put(
                                  "$instanceIsOfNative",
                                  MethodInfo.newBuilder()
                                      .setReturnType(TypeDescriptors.get().javaLangBoolean)
                                      .setParameters(TypeDescriptors.get().javaLangObject)
                                      .build())
                              .put(
                                  "$stampType",
                                  MethodInfo.newBuilder()
                                      .setReturnType(TypeDescriptors.get().javaLangObjectArray)
                                      .setParameters(
                                          TypeDescriptors.get().javaLangObjectArray,
                                          TypeDescriptors.get().javaLangObject,
                                          PrimitiveTypes.DOUBLE)
                                      .build())
                              .build())
                      .put(
                          BootstrapType.NATIVE_UTIL.getDescriptor(),
                          // Util methods
                          ImmutableMap.<String, MethodInfo>builder()
                              .put(
                                  "$assertClinit",
                                  MethodInfo.newBuilder()
                                      .setReturnType(PrimitiveTypes.VOID)
                                      .setParameters(TypeDescriptors.get().javaLangObject)
                                      .build())
                              .put(
                                  "$makeLambdaFunction",
                                  MethodInfo.newBuilder()
                                      .setReturnType(TypeDescriptors.get().nativeFunction)
                                      .setParameters(
                                          TypeDescriptors.get().nativeFunction,
                                          TypeDescriptors.get().javaLangObject,
                                          TypeDescriptors.get().nativeFunction)
                                      .build())
                              .put(
                                  "$makeEnumName",
                                  MethodInfo.newBuilder()
                                      .setReturnType(TypeDescriptors.get().javaLangString)
                                      .setParameters(TypeDescriptors.get().javaLangString)
                                      .build())
                              .put(
                                  "$setClassMetadata",
                                  MethodInfo.newBuilder()
                                      .setReturnType(PrimitiveTypes.VOID)
                                      .setParameters(
                                          TypeDescriptors.get().javaemulInternalConstructor,
                                          TypeDescriptors.get().javaLangString)
                                      .build())
                              .put(
                                  "$setClassMetadataForInterface",
                                  MethodInfo.newBuilder()
                                      .setReturnType(PrimitiveTypes.VOID)
                                      .setParameters(
                                          TypeDescriptors.get().javaemulInternalConstructor,
                                          TypeDescriptors.get().javaLangString)
                                      .build())
                              .put(
                                  "$setClassMetadataForEnum",
                                  MethodInfo.newBuilder()
                                      .setReturnType(PrimitiveTypes.VOID)
                                      .setParameters(
                                          TypeDescriptors.get().javaemulInternalConstructor,
                                          TypeDescriptors.get().javaLangString)
                                      .build())
                              .put(
                                  "$synchronized",
                                  MethodInfo.newBuilder()
                                      .setReturnType(PrimitiveTypes.VOID)
                                      .setParameters(TypeDescriptors.get().javaLangObject)
                                      .build())
                              .build())
                      .put(
                          BootstrapType.NATIVE_EQUALITY.getDescriptor(),
                          // Util methods
                          ImmutableMap.<String, MethodInfo>builder()
                              .put(
                                  "$same",
                                  MethodInfo.newBuilder()
                                      .setReturnType(PrimitiveTypes.BOOLEAN)
                                      .setParameters(
                                          TypeDescriptors.get().javaLangObject,
                                          TypeDescriptors.get().javaLangObject)
                                      .build())
                              .build())
                      .put(
                          BootstrapType.LONG_UTILS.getDescriptor(),
                          // LongUtils methods
                          ImmutableMap.<String, MethodInfo>builder()
                              .put(
                                  "negate",
                                  MethodInfo.newBuilder()
                                      .setReturnType(PrimitiveTypes.LONG)
                                      .setParameters(PrimitiveTypes.LONG)
                                      .build())
                              .put(
                                  "not",
                                  MethodInfo.newBuilder()
                                      .setReturnType(PrimitiveTypes.LONG)
                                      .setParameters(PrimitiveTypes.LONG)
                                      .build())
                              .build())
                      .put(
                          BootstrapType.NATIVE_LONG.getDescriptor(),
                          // goog.math.long methods
                          ImmutableMap.<String, MethodInfo>builder()
                              .put(
                                  "fromInt",
                                  MethodInfo.newBuilder()
                                      .setReturnType(PrimitiveTypes.LONG)
                                      .setParameters(PrimitiveTypes.INT)
                                      .build())
                              .put(
                                  "fromBits",
                                  MethodInfo.newBuilder()
                                      .setReturnType(PrimitiveTypes.LONG)
                                      .setParameters(PrimitiveTypes.INT, PrimitiveTypes.INT)
                                      .build())
                              .build())
                      .build());

  /** Create a call to a J2cl runtime method. */
  private static MethodCall createRuntimeMethodCall(
      DeclaredTypeDescriptor vmTypeDescriptor, String methodName, List<Expression> arguments) {
    MethodInfo methodInfo =
        runtimeMethodInfoByMethodNameByType.get().get(vmTypeDescriptor).get(methodName);
    checkNotNull(methodInfo, "%s#%s(%s)", vmTypeDescriptor, methodName, arguments);
    List<TypeDescriptor> parameterTypeDescriptors = methodInfo.getParameters();
    int requiredParameters = methodInfo.getRequiredParameters();
    TypeDescriptor returnTypeDescriptor = methodInfo.getReturnType();

    checkArgument(arguments.size() >= requiredParameters);

    MethodDescriptor methodDescriptor =
        MethodDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(vmTypeDescriptor)
            .setJsInfo(JsInfo.RAW)
            .setStatic(true)
            .setName(methodName)
            .setParameterTypeDescriptors(parameterTypeDescriptors.subList(0, arguments.size()))
            .setReturnTypeDescriptor(returnTypeDescriptor)
            .build();
    // Use the raw type as the stamped leaf type. So that we use the upper bound of a generic type
    // parameter type instead of the type parameter itself.
    return MethodCall.Builder.from(methodDescriptor).setArguments(arguments).build();
  }

  @AutoValue
  abstract static class MethodInfo {
    public abstract TypeDescriptor getReturnType();

    public abstract int getRequiredParameters();

    public abstract ImmutableList<TypeDescriptor> getParameters();

    public static Builder newBuilder() {
      return new AutoValue_RuntimeMethods_MethodInfo.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      public abstract Builder setReturnType(TypeDescriptor returnType);

      public abstract Builder setRequiredParameters(int requiredParameters);

      public abstract Builder setParameters(TypeDescriptor... parameters);

      public abstract MethodInfo autoBuild();

      abstract OptionalInt getRequiredParameters();

      abstract ImmutableList<TypeDescriptor> getParameters();

      public MethodInfo build() {
        if (!getRequiredParameters().isPresent()) {
          setRequiredParameters(getParameters().size());
        }
        MethodInfo methodInfo = autoBuild();
        checkArgument(
            methodInfo.getRequiredParameters() >= 0
                && methodInfo.getRequiredParameters() <= methodInfo.getParameters().size());
        return methodInfo;
      }
    }
  }
}
