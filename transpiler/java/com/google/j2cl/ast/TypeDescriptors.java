/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.ast;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * Utility class holding type descriptors that need to be referenced directly.
 */
public class TypeDescriptors {
  public static final TypeDescriptor BOOLEAN_TYPE_DESCRIPTOR =
      TypeDescriptor.createPrimitive(TypeDescriptor.BOOLEAN_TYPE_NAME);
  public static final TypeDescriptor BYTE_TYPE_DESCRIPTOR =
      TypeDescriptor.createPrimitive(TypeDescriptor.BYTE_TYPE_NAME);
  public static final TypeDescriptor SHORT_TYPE_DESCRIPTOR =
      TypeDescriptor.createPrimitive(TypeDescriptor.SHORT_TYPE_NAME);
  public static final TypeDescriptor INT_TYPE_DESCRIPTOR =
      TypeDescriptor.createPrimitive(TypeDescriptor.INT_TYPE_NAME);
  public static final TypeDescriptor LONG_TYPE_DESCRIPTOR =
      TypeDescriptor.createPrimitive(TypeDescriptor.LONG_TYPE_NAME);
  public static final TypeDescriptor FLOAT_TYPE_DESCRIPTOR =
      TypeDescriptor.createPrimitive(TypeDescriptor.FLOAT_TYPE_NAME);
  public static final TypeDescriptor DOUBLE_TYPE_DESCRIPTOR =
      TypeDescriptor.createPrimitive(TypeDescriptor.DOUBLE_TYPE_NAME);
  public static final TypeDescriptor CHAR_TYPE_DESCRIPTOR =
      TypeDescriptor.createPrimitive(TypeDescriptor.CHAR_TYPE_NAME);
  public static final TypeDescriptor VOID_TYPE_DESCRIPTOR =
      TypeDescriptor.createPrimitive(TypeDescriptor.VOID_TYPE_NAME);
  public static final TypeDescriptor WILD_CARD_TYPE_DESCRIPTOR =
      TypeDescriptor.createTypeVariable(new ArrayList<String>(), Arrays.asList("?"), "");
  public static final TypeDescriptor CLASS_TYPE_DESCRIPTOR =
      TypeDescriptor.create(Arrays.asList("java", "lang"), Arrays.asList("Class"), "Class");
  public static final TypeDescriptor OBJECT_TYPE_DESCRIPTOR =
      TypeDescriptor.create(Arrays.asList("java", "lang"), Arrays.asList("Object"), "Object");
  public static final TypeDescriptor STRING_TYPE_DESCRIPTOR =
      TypeDescriptor.create(Arrays.asList("java", "lang"), Arrays.asList("String"), "String");

  /**
   * Bootstrap types.
   */
  public static final TypeDescriptor OBJECTS_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Objects", "ObjectsModule");

  public static final TypeDescriptor NUMBERS_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Numbers", "NumbersModule");
  public static final TypeDescriptor BYTES_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Bytes", "BytesModule");
  public static final TypeDescriptor DOUBLES_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Doubles", "DoublesModule");
  public static final TypeDescriptor FLOATS_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Floats", "FloatsModule");
  public static final TypeDescriptor INTEGERS_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Integers", "IntegersModule");
  public static final TypeDescriptor LONGS_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Longs", "LongsModule");
  public static final TypeDescriptor SHORTS_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Shorts", "ShortsModule");

  public static final TypeDescriptor NATIVE_UTIL_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("nativebootstrap"), "Util", "UtilModule");
  public static final TypeDescriptor VM_ASSERTS_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Asserts", "AssertsModule");
  public static final TypeDescriptor VM_ARRAYS_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Arrays", "ArraysModule");
  public static final TypeDescriptor VM_CASTS_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Casts", "CastsModule");
  public static final TypeDescriptor VM_PRIMITIVES_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("vmbootstrap"), "Primitives", "PrimitivesModule");
  public static final TypeDescriptor NATIVE_LONGS_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("nativebootstrap"), "LongUtils", "LongUtilsModule");
  public static final TypeDescriptor NATIVE_LONG_TYPE_DESCRIPTOR =
      TypeDescriptor.createRaw(Arrays.asList("nativebootstrap"), "Long", "LongUtilsModule");

  /**
   * Bootstrap type set.
   */
  public static final Set<TypeDescriptor> bootstrapTypeDescriptors =
      ImmutableSet.of(
          NATIVE_UTIL_TYPE_DESCRIPTOR,
          VM_ASSERTS_TYPE_DESCRIPTOR,
          VM_ARRAYS_TYPE_DESCRIPTOR,
          VM_CASTS_TYPE_DESCRIPTOR,
          VM_PRIMITIVES_TYPE_DESCRIPTOR,
          NATIVE_LONGS_TYPE_DESCRIPTOR,
          NATIVE_LONG_TYPE_DESCRIPTOR,
          OBJECTS_TYPE_DESCRIPTOR,
          NUMBERS_TYPE_DESCRIPTOR,
          BYTES_TYPE_DESCRIPTOR,
          DOUBLES_TYPE_DESCRIPTOR,
          FLOATS_TYPE_DESCRIPTOR,
          INTEGERS_TYPE_DESCRIPTOR,
          LONGS_TYPE_DESCRIPTOR,
          SHORTS_TYPE_DESCRIPTOR);

  private TypeDescriptors() {}
}
