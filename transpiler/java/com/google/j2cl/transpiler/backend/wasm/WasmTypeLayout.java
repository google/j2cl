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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Origin;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** Runtime representation of a Java class in Wasm. */
@AutoValue
abstract class WasmTypeLayout {
  /** Create a layout for a type that is declared the library being compiled. */
  static WasmTypeLayout createFromType(Type javaType, WasmTypeLayout wasmSupertypeLayout) {
    return new AutoValue_WasmTypeLayout(
        javaType, javaType.getTypeDescriptor(), wasmSupertypeLayout);
  }

  /** Create a layout for a type that is declared in a different library. */
  static WasmTypeLayout createFromTypeDeclaration(
      TypeDeclaration typeDeclaration, WasmTypeLayout wasmSupertypeLayout) {
    return new AutoValue_WasmTypeLayout(null, typeDeclaration.toDescriptor(), wasmSupertypeLayout);
  }

  /**
   * The Java class represented by this Wasm type if the type is in the current library, {@code
   * null} if the type is from a different library.
   */
  @Nullable
  abstract Type getJavaType();

  abstract DeclaredTypeDescriptor getTypeDescriptor();

  TypeDeclaration getTypeDeclaration() {
    return getTypeDescriptor().getTypeDeclaration();
  }

  /** The wasm representation of the superclass for this Java class. */
  @Nullable
  abstract WasmTypeLayout getWasmSupertypeLayout();

  /** Returns all the fields that will be in the layout for struct for the Java class. */
  @Memoized
  Collection<FieldDescriptor> getAllInstanceFields() {

    WasmTypeLayout wasmSupertypeLayout = getWasmSupertypeLayout();
    List<FieldDescriptor> instanceFields = new ArrayList<>();
    if (wasmSupertypeLayout != null) {
      instanceFields.addAll(wasmSupertypeLayout.getAllInstanceFields());
    }

    ImmutableList<FieldDescriptor> declaredInstanceFields = getDeclaredInstanceFields();

    if (TypeDescriptors.isWasmArraySubtype(getTypeDescriptor())) {
      // TODO(b/296475021): Remove the hack to treat the field as overriden by subclass' field.
      // Override the type of the elements field in Wasm arrays by replacing the WasmArray elements
      // field with that of their subtype.
      // Relies on the elements field being the last declared filed in WasmArray and also being
      // the first in the WasmArray subclass.
      checkState(declaredInstanceFields.get(0).getName().equals("elements"));
      FieldDescriptor removedField = instanceFields.remove(instanceFields.size() - 1);
      checkState(removedField.getName().equals("elements"));
    }

    instanceFields.addAll(declaredInstanceFields);

    return instanceFields;
  }

  private ImmutableList<FieldDescriptor> getDeclaredInstanceFields() {
    Type type = getJavaType();
    Stream<FieldDescriptor> declaredFieldDescriptors;
    if (type != null) {
      // If the type is in the library, just look at the field instances in the AST of the Type.
      // In this scenario we will see exactly the instance fields even the ones that are synthesized
      // for captures.
      declaredFieldDescriptors = type.getInstanceFields().stream().map(Field::getDescriptor);
    } else {
      // If the type is not in the library, look at the declared fields for the type in the type
      // model; in this case we will only see the instance fields that were declared on the type,
      // but not the synthetic ones.
      declaredFieldDescriptors =
          getTypeDescriptor().getDeclaredFieldDescriptors().stream()
              .filter(FieldDescriptor::isInstanceMember);

      // The only synthetic field we care for types that are not in the current library is the field
      // to store the enclosing instance which we can add it explicitly here if needed. In Wasm
      // declaring structures and initializing instances requires knowing the full structure of the
      // type, which includes all the fields inherited from supertypes including private fields.
      // Luckily in Java, types that have other captures can not be subclasses outside the library,
      // and due to the fact that initialization is behind a factory method, the structures of these
      // types are never leaked outside the compilation unit the type is defined in.
      if (getTypeDescriptor().getTypeDeclaration().isCapturingEnclosingInstance()) {
        declaredFieldDescriptors =
            Stream.concat(
                declaredFieldDescriptors,
                Stream.of(getTypeDescriptor().getFieldDescriptorForEnclosingInstance()));
      }
    }

    return declaredFieldDescriptors
        // Declared fields are sorted by mangled name because they might not appear in the same
        // order in the AST.
        .sorted(Comparator.comparing(FieldDescriptor::getMangledName))
        .collect(toImmutableList());
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
      // Add all the methods from the super type layout to ensure that they appear in the same
      // order as in the superclass.
      instanceMethodsByMangledName.putAll(
          getWasmSupertypeLayout().getAllPolymorphicMethodsByMangledName());
    }
    DeclaredTypeDescriptor typeDescriptor = getTypeDescriptor();
    typeDescriptor.getPolymorphicMethods().stream()
        .filter(this::needsVtableEntry)
        .sorted(Comparator.comparing(MethodDescriptor::getMangledName))
        .forEach(md -> instanceMethodsByMangledName.put(md.getMangledName(), md));
    // Patch entry for $getClassImpl. In the type model there is only `Object::$getClasImpl` and
    // that is what is returned by `getPolymorphicMethod()`. But it is overridden in the AST
    // in a predictable manner by the pass that synthesizes the overrides and needs to be explicitly
    // patched here.
    if (!typeDescriptor.isInterface()) {
      MethodDescriptor getClassMethodDescriptor =
          getGetClassMethodDescriptor(
              typeDescriptor.getTypeDeclaration().getOrigin() == Origin.LAMBDA_IMPLEMENTOR
                  ? typeDescriptor.getSuperTypeDescriptor()
                  : typeDescriptor);
      instanceMethodsByMangledName.put(
          getClassMethodDescriptor.getMangledName(), getClassMethodDescriptor);
    }
    return instanceMethodsByMangledName;
  }

  private boolean needsVtableEntry(MethodDescriptor methodDescriptor) {
    if (getTypeDescriptor().isInterface() && methodDescriptor.isOrOverridesJavaLangObjectMethod()) {
      // `j.l.Object` methods and their overrides are never dispatched through interfaces.
      // Hence they should not be included in any interface vtable (even if the interface
      // redeclares them).
      return false;
    }
    return !(methodDescriptor.getEnclosingTypeDescriptor().isFinal() || methodDescriptor.isFinal())
        // TODO(b/342007699): Consider a separate method instead of
        // getJsOverriddenMethodDescriptors.
        || !methodDescriptor.getJsOverriddenMethodDescriptors().isEmpty()
        || isAccidentalInterfaceOverride(methodDescriptor);
  }

  private boolean isAccidentalInterfaceOverride(MethodDescriptor methodDescriptor) {
    return getTypeDescriptor().getInterfaceTypeDescriptors().stream()
        .flatMap(i -> i.getPolymorphicMethods().stream())
        // Skip the methods interfaces inherit from java.lang.Object.
        .filter(m -> m.getEnclosingTypeDescriptor().isInterface())
        .anyMatch(m -> m.getMangledName().equals(methodDescriptor.getMangledName()));
  }

  private static MethodDescriptor getGetClassMethodDescriptor(
      DeclaredTypeDescriptor typeDescriptor) {
    return MethodDescriptor.Builder.from(
            TypeDescriptors.get().javaLangObject.getMethodDescriptor("$getClassImpl"))
        .setEnclosingTypeDescriptor(typeDescriptor)
        .setSynthetic(true)
        .build();
  }
}
