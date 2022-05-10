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
  int compileTimeConstant = 12;
  String stringLiteral = "Hello World";
  Object[] newArray = new Object[4];
  Object[][][] newArrayMultidimensional = new Object[3][4][];
  int[] arrayLiteral = {1, 2, 3, 4};
  int[][] arrayLiteralMultidimensional = {{1, 2}, {3, 4}};
  Class<?> classLiteral = WasmImmutableFields.class;
  String[] withCast = (String[]) (Object) new Object[0];
}

class AssignmentsInInitializer {
  int compileTimeConstant;
  String stringLiteral;
  Object[] newArray;
  Object[][][] newArrayMultidimensional;
  int[] arrayLiteral;
  int[][] arrayLiteralMultidimensional;
  Class<?> classLiteral;
  String[] withCast;

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
  int compileTimeConstant;
  String stringLiteral;
  Object[] newArray;
  Object[][][] newArrayMultidimensional;
  int[] arrayLiteral;
  int[][] arrayLiteralMultidimensional;
  Class<?> classLiteral;
  String[] withCast;
  Object fromParameter;
  String finalFieldReference;

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
  int optimizeableField;

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

class FinalizerTest {
  int effectivelyFinalField = 1;
  int readInInit = effectivelyFinalField; // A valid read.

  int writtenOutsideConstructor = 1;
  int readBeforeWrite;
  int writtenInDelegatingConstructor = 1;
  int compoundAssignment;
  int effectivelyFinalButNotOptimizedIfBranches;
  int effectivelyFinalButNotOptimizedNestedBlock;
  int writtenInInitAndConstructor = 1;
  int writtenInInitializerAndInDelegatingConstructor = 1;
  int writtenOnlyInDelegatingConstructor;

  FinalizerTest() {
    readBeforeWrite = readBeforeWrite + 1;

    // A valid read.
    int i = effectivelyFinalField;

    compoundAssignment++;
    if (i == 3) {
      effectivelyFinalButNotOptimizedIfBranches = 1;
    } else {
      effectivelyFinalButNotOptimizedIfBranches = 1;
    }
    {
      effectivelyFinalButNotOptimizedNestedBlock = 1;
    }
    writtenInInitAndConstructor = 1;
  }

  FinalizerTest(int x) {
    this();

    // A valid read.
    int i = effectivelyFinalField;

    writtenInDelegatingConstructor = 1;
    writtenOnlyInDelegatingConstructor = 1;
    writtenInInitializerAndInDelegatingConstructor = 1;
  }

  void foo() {
    FinalizerTest o = null;
    o.writtenOutsideConstructor = 1;

    // A valid read.
    int i = effectivelyFinalField;
  }
}
