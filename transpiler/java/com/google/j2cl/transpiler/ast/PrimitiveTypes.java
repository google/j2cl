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

import com.google.common.collect.ImmutableList;

/** Exhaustive enumeration of primitive types. */
public final class PrimitiveTypes {
  public static final PrimitiveTypeDescriptor VOID =
      new PrimitiveTypeDescriptor("void", "V", "java.lang.Void", -1, -1);
  public static final PrimitiveTypeDescriptor BOOLEAN =
      new PrimitiveTypeDescriptor("boolean", "Z", "java.lang.Boolean", -1, 8);
  public static final PrimitiveTypeDescriptor BYTE =
      new PrimitiveTypeDescriptor("byte", "B", "java.lang.Byte", 1, 8);
  public static final PrimitiveTypeDescriptor SHORT =
      new PrimitiveTypeDescriptor("short", "S", "java.lang.Short", 2, 16);
  public static final PrimitiveTypeDescriptor CHAR =
      new PrimitiveTypeDescriptor("char", "C", "java.lang.Character", 2, 16);
  public static final PrimitiveTypeDescriptor INT =
      new PrimitiveTypeDescriptor("int", "I", "java.lang.Integer", 3, 32);
  public static final PrimitiveTypeDescriptor LONG =
      new PrimitiveTypeDescriptor("long", "J", "java.lang.Long", 4, 64);
  public static final PrimitiveTypeDescriptor FLOAT =
      new PrimitiveTypeDescriptor("float", "F", "java.lang.Float", 5, 32);
  public static final PrimitiveTypeDescriptor DOUBLE =
      new PrimitiveTypeDescriptor("double", "D", "java.lang.Double", 6, 64);

  public static final ImmutableList<PrimitiveTypeDescriptor> TYPES =
      ImmutableList.of(VOID, BOOLEAN, BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE);

  /** Returns the instance corresponding to the name. */
  public static PrimitiveTypeDescriptor get(String name) {
    return TYPES
        .stream()
        .filter(
            primitiveTypeDescriptor -> name.equals(primitiveTypeDescriptor.getSimpleSourceName()))
        .findFirst()
        .get();
  }

  private PrimitiveTypes() {}
}
