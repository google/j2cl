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
package com.google.j2cl.integration.autovalue_ref;

final class CompositeA {
  private final int intField;
  private final boolean booleanField;
  private final String stringField;
  private final Double doubleField;
  private final ComponentA componentField;

  public CompositeA(
      int intField,
      boolean booleanField,
      String stringField,
      Double doubleField,
      ComponentA componentField) {
    this.intField = intField;
    this.booleanField = booleanField;
    this.stringField = stringField;
    this.doubleField = doubleField;
    this.componentField = componentField;
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

  public ComponentA getComponentField() {
    return componentField;
  }

  @Override
  public String toString() {
    return "CompositeA{"
        + "intField="
        + intField
        + ", booleanField="
        + booleanField
        + ", stringField='"
        + stringField
        + '\''
        + ", doubleField="
        + doubleField
        + ", componentField="
        + componentField
        + '}';
  }
}
