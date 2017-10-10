/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.transpiler.readable.extendedoperands;

// This is a readable example that exposes the nuances of JDT representation of InfixExpression,
// where certain expressions are represented as operator, list of operands rather than a
// nested binary tree structure.
public class ExtendedOperands {

  @SuppressWarnings("unused")
  public void test() {
    Integer boxedInteger = 3;
    int i;
    long l;
    double d;
    l = 2 - boxedInteger - 2L;
    l = 2 | boxedInteger | 2L;
    l = 1000000L * l * 60 * 60 * 24;
    l = 24 * 60 * 60 * l * 1000000L;
    d = l = i = 20;
    l = boxedInteger = i = 20;
    l = i + boxedInteger + l + 20;
    d = 20 + l + d;
  }
}
