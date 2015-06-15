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
package com.google.j2cl.frontend;

import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.TypeReference;

import java.util.HashMap;
import java.util.Map;

/**
 * Global map from TypeReference to the corresponding JavaType.
 */
public class TypeMap {

  private static Map<TypeReference, JavaType> typeMap = new HashMap<>();

  public static void put(TypeReference typeReference, JavaType type) {
    typeMap.put(typeReference, type);
  }

  public static JavaType get(TypeReference typeReference) {
    return typeMap.get(typeReference);
  }
}
