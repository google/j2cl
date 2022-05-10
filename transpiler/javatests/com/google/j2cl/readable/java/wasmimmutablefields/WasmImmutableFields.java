/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wasmimmutablefields;

public class WasmImmutableFields {}

class AssignmentsInDeclaration {
  final int compileTimeConstant = 12;
  final String stringLiteral = "Hello World";
  final Object[] newArray = new Object[4];
  final Object[][][] newArrayMultidimensional = new Object[3][4][];
  final int[] arrayLiteral = {1, 2, 3, 4};
  final int[][] arrayLiteralMultidimensional = {{1, 2}, {3, 4}};
  final Class<?> classLiteral = WasmImmutableFields.class;
  final String[] withCast = (String[]) (Object) new Object[0];
}

class AssignmentsInInitializer {
  final int compileTimeConstant;
  final String stringLiteral;
  final Object[] newArray;
  final Object[][][] newArrayMultidimensional;
  final int[] arrayLiteral;
  final int[][] arrayLiteralMultidimensional;
  final Class<?> classLiteral;
  final String[] withCast;

  {
    compileTimeConstant = 12;
    stringLiteral = "Hello World";
    newArray = new Object[4];
    newArrayMultidimensional = new Object[3][4][];
    arrayLiteral = new int[] {1, 2, 3, 4};
    arrayLiteralMultidimensional = new int[][] {{1, 2}, {3, 4}};
    classLiteral = WasmImmutableFields.class;
    withCast = (String[]) (Object) new Object[0];
  }
}

class AssignmentsInConstructor {
  final int compileTimeConstant;
  final String stringLiteral;
  final Object[] newArray;
  final Object[][][] newArrayMultidimensional;
  final int[] arrayLiteral;
  final int[][] arrayLiteralMultidimensional;
  final Class<?> classLiteral;
  final String[] withCast;
  final Object fromParameter;
  final String finalFieldReference;

  AssignmentsInConstructor(Object parameter, int intParameter, AssignmentsInConstructor par) {
    compileTimeConstant = 12;
    stringLiteral = "Hello World";
    newArray = new Object[intParameter];
    newArrayMultidimensional = new Object[intParameter][4][];
    arrayLiteral = new int[] {1, 2, intParameter, 4};
    arrayLiteralMultidimensional = new int[][] {{1, intParameter}, {3, 4}};
    classLiteral = WasmImmutableFields.class;
    withCast = (String[]) (Object) new Object[0];
    fromParameter = parameter;
    finalFieldReference = par.stringLiteral;
  }
}

class OptimizesWithThisConstructorDelegation {
  final int optimizeableField;

  OptimizesWithThisConstructorDelegation() {
    optimizeableField = 2;
  }

  OptimizesWithThisConstructorDelegation(int parameter) {
    optimizeableField = parameter;
  }

  OptimizesWithThisConstructorDelegation(short parameter) {
    // Delegation using this does not prevent optimization.
    this((int) parameter);
  }
}

class OptimizesWithSuperConstructorDelegation extends OptimizesWithThisConstructorDelegation {
  OptimizesWithSuperConstructorDelegation() {}

  OptimizesWithSuperConstructorDelegation(int parameter) {
    // Delegation using super does not prevent optimization.
    super(parameter);
  }

  OptimizesWithSuperConstructorDelegation(short parameter) {
    this((int) parameter);
  }
}
