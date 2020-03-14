/*
 * Copyright 2020 Google Inc.
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
package com.google.j2cl.transpiler.integration.autovalue_ref;

import java.util.Arrays;
import java.util.Objects;

final class ComponentA {
  private final int intField;
  private final boolean booleanField;
  private final String stringField;
  private final Double doubleField;
  private final int[] arrayField;

  public ComponentA(
      int intField,
      boolean booleanField,
      String stringField,
      Double doubleField,
      int[] arrayField) {
    this.intField = intField;
    this.booleanField = booleanField;
    this.stringField = stringField;
    this.doubleField = doubleField;
    this.arrayField = arrayField;
  }

  public int getIntField() {
    return intField;
  }

  public boolean getBooleanField() {
    return booleanField;
  }

  public String getStringField() {
    return stringField;
  }

  public Double getDoubleField() {
    return doubleField;
  }

  public int[] getArrayField() {
    return arrayField;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentA that = (ComponentA) o;
    return intField == that.intField
        && booleanField == that.booleanField
        && Objects.equals(stringField, that.stringField)
        && Objects.equals(doubleField, that.doubleField)
        && Arrays.equals(arrayField, that.arrayField);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(intField, booleanField, stringField, doubleField);
    result = 31 * result + Arrays.hashCode(arrayField);
    return result;
  }

  @Override
  public String toString() {
    return "ComponentA{"
        + "intField="
        + intField
        + ", booleanField="
        + booleanField
        + ", stringField='"
        + stringField
        + '\''
        + ", doubleField="
        + doubleField
        + ", arrayField="
        + Arrays.toString(arrayField)
        + '}';
  }
}
