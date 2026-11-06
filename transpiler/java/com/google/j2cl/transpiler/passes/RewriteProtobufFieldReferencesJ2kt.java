/*
 * Copyright 2026 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import javax.annotation.Nullable;

/**
 * Rewrites references to static fields declared on package-private shard interfaces of large
 * protobuf enums (such as {@code large_enum} or {@code bigjavaenum}) to be qualified by the public
 * protobuf enum class instead of the shard interface.
 *
 * <p>In Java, large protobuf enums split their static constants across package-private shard
 * interfaces ({@code Foo0}, {@code Foo1}, ...) implemented by the public enum class {@code Foo}, so
 * javac resolves {@code Foo.BAR} and {@code Foo.BAR_VALUE} to {@code Foo0.BAR} and {@code
 * Foo0.BAR_VALUE}. In Kotlin, the shard interfaces are either package-private (on JVM) or absent
 * altogether (on Kotlin/Native, where all entries are generated directly on {@code Foo}), so
 * references must be qualified by {@code Foo}.
 */
// TODO(b/581990423): Remove in favor of a more general solution.
public final class RewriteProtobufFieldReferencesJ2kt extends NormalizationPass {

  private static final String VALUE_FIELD_SUFFIX = "_VALUE";

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor fieldDescriptor = fieldAccess.getTarget();
            DeclaredTypeDescriptor enumTypeDescriptor =
                findProtobufEnumTypeDescriptor(fieldDescriptor);
            if (enumTypeDescriptor != null) {
              return rewriteProtobufEnumFieldAccess(fieldAccess, enumTypeDescriptor);
            }
            return fieldAccess;
          }
        });
  }

  /**
   * Rewrites a static field access on a package-private protobuf large-enum shard interface (such
   * as {@code Foo0.BAR} or {@code Foo0.BAR_VALUE}) to target the public protobuf enum class ({@code
   * Foo.BAR} or {@code Foo.BAR_VALUE}) with a non-nullable type (since {@code @ProtoNonnullApi} is
   * only applied to {@code Foo} and not to the shard interface {@code Foo0}).
   */
  private static FieldAccess rewriteProtobufEnumFieldAccess(
      FieldAccess fieldAccess, DeclaredTypeDescriptor enumTypeDescriptor) {
    FieldDescriptor fieldDescriptor = fieldAccess.getTarget();
    return fieldAccess.toBuilder()
        .setTarget(
            fieldDescriptor.transform(
                builder ->
                    builder
                        .setEnclosingTypeDescriptor(enumTypeDescriptor.toNonNullable())
                        .setTypeDescriptor(fieldDescriptor.getTypeDescriptor().toNonNullable())))
        .build();
  }

  /**
   * Matches static constants declared on a package-private protobuf large-enum shard interface
   * (either an enum constant {@code Foo BAR} or its numeric wire-value constant {@code int
   * BAR_VALUE}) and returns the type descriptor of the public protobuf enum class {@code Foo}, or
   * {@code null} if {@code fieldDescriptor} does not match.
   */
  @Nullable
  private static DeclaredTypeDescriptor findProtobufEnumTypeDescriptor(
      FieldDescriptor fieldDescriptor) {
    // Bigenum constants are always static fields.
    if (!fieldDescriptor.isStatic()) {
      return null;
    }

    DeclaredTypeDescriptor enclosingTypeDescriptor = fieldDescriptor.getEnclosingTypeDescriptor();
    if (!enclosingTypeDescriptor.isInterface()
        || !enclosingTypeDescriptor.getTypeDeclaration().getVisibility().isPackagePrivate()) {
      return null;
    }

    // This is a static field on a package-private interface. Find the actual subclass that exposes
    // it. Since we are targeting bigenums, for enum constants (`Foo BAR`), the type of the field is
    // the type that exposes it.
    if (fieldDescriptor.getTypeDescriptor() instanceof DeclaredTypeDescriptor fieldType
        && isProtobufEnumImplementingShard(fieldType, enclosingTypeDescriptor)) {
      // Here is where we return the type descriptor for the bigenum itself.
      return fieldType;
    }

    // Each shard interface also declares an `int <NAME>_VALUE` constant for each enum constant
    // `<NAME>` (e.g. `int BAR_VALUE = 1;` alongside `Foo BAR = ...`). Since `BAR_VALUE` has type
    // `int` rather than `Foo`, resolve the bigenum type from the corresponding `<NAME>` field on
    // the same shard interface.
    if (TypeDescriptors.isPrimitiveInt(fieldDescriptor.getTypeDescriptor())
        && fieldDescriptor.getName().endsWith(VALUE_FIELD_SUFFIX)) {
      String enumConstantName =
          fieldDescriptor
              .getName()
              .substring(0, fieldDescriptor.getName().length() - VALUE_FIELD_SUFFIX.length());
      return enclosingTypeDescriptor.getDeclaredFieldDescriptors().stream()
          .filter(f -> f.getName().equals(enumConstantName))
          .map(FieldDescriptor::getTypeDescriptor)
          .filter(DeclaredTypeDescriptor.class::isInstance)
          .map(DeclaredTypeDescriptor.class::cast)
          .filter(t -> isProtobufEnumImplementingShard(t, enclosingTypeDescriptor))
          .findFirst()
          .orElse(null);
    }

    return null;
  }

  private static boolean isProtobufEnumImplementingShard(
      DeclaredTypeDescriptor candidateType, DeclaredTypeDescriptor shardInterfaceType) {
    return !candidateType.isInterface()
        && candidateType.isSubtypeOf(shardInterfaceType)
        && isEnumLite(candidateType.getTypeDeclaration());
  }

  private static boolean isEnumLite(TypeDeclaration typeDeclaration) {
    return typeDeclaration.getAllSuperInterfaces().stream()
        .map(TypeDeclaration::getQualifiedSourceName)
        .anyMatch("com.google.protobuf.Internal.EnumLite"::equals);
  }
}
