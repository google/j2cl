/*
 * Copyright 2021 Google Inc.
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

package com.google.j2cl.transpiler.backend.wasm;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** Runtime representation of a Java class in Wasm. */
@AutoValue
abstract class WasmTypeLayout {

  static WasmTypeLayout create(Type javaType, WasmTypeLayout wasmSupertypeLayout) {
    return new AutoValue_WasmTypeLayout(javaType, wasmSupertypeLayout);
  }

  /** The Java class represented by this Wasm type. */
  abstract Type getJavaType();
  /** The wasm representation of the superclass for this Java class. */
  @Nullable
  abstract WasmTypeLayout getWasmSupertypeLayout();

  /** Returns all the fields that will be in the layout for struct for the Java class. */
  @Memoized
  Collection<Field> getAllInstanceFields() {
    Set<Field> instanceFields = new LinkedHashSet<>();
    if (getWasmSupertypeLayout() != null) {
      instanceFields.addAll(getWasmSupertypeLayout().getAllInstanceFields());
    }
    instanceFields.addAll(getJavaType().getInstanceFields());
    return instanceFields;
  }

  /** Returns all the methods that will be part of the vtable for the Java class. */
  Collection<MethodDescriptor> getAllPolymorphicMethods() {
    return getAllPolymorphicMethodsByMangledName().values();
  }

  /** Returns the descriptor for the method implementing {@code methodDescriptor} in this type. */
  MethodDescriptor getImplementationMethod(MethodDescriptor methodDescriptor) {
    return getAllPolymorphicMethodsByMangledName().get(methodDescriptor.getMangledName());
  }

  /**
   * All the polymorphic methods that need to be in the vtable for the class.
   *
   * <p>These are all the instance methods for this class and its superclasses, excluding the
   * private methods that do not require dynamic dispatch.
   */
  @Memoized
  Map<String, MethodDescriptor> getAllPolymorphicMethodsByMangledName() {
    Map<String, MethodDescriptor> instanceMethodsByMangledName = new LinkedHashMap<>();
    if (getWasmSupertypeLayout() != null) {
      instanceMethodsByMangledName.putAll(
          getWasmSupertypeLayout().getAllPolymorphicMethodsByMangledName());
    }
    for (Method method : getJavaType().getMethods()) {
      MethodDescriptor methodDescriptor = method.getDescriptor();
      if (methodDescriptor.isPolymorphic()) {
        instanceMethodsByMangledName.put(methodDescriptor.getMangledName(), methodDescriptor);
      }
    }
    return instanceMethodsByMangledName;
  }
}
