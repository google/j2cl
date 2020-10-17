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
package com.google.j2cl.transpiler.backend.wasm;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Generates a WASM module containing all the code for the application. */
public class WasmModuleGenerator {

  private static final ImmutableMap<PrimitiveTypeDescriptor, String> WASM_TYPES_BY_PRIMITIVE_TYPES =
      ImmutableMap.<PrimitiveTypeDescriptor, String>builder()
          .put(PrimitiveTypes.BOOLEAN, "i32")
          .put(PrimitiveTypes.BYTE, "i32")
          .put(PrimitiveTypes.CHAR, "i32")
          .put(PrimitiveTypes.SHORT, "i32")
          .put(PrimitiveTypes.INT, "i32")
          .put(PrimitiveTypes.LONG, "i64")
          .put(PrimitiveTypes.FLOAT, "f32")
          .put(PrimitiveTypes.DOUBLE, "f64")
          .build();

  private final Problems problems;
  private final Path outputPath;
  private final SourceBuilder builder = new SourceBuilder();
  /**
   * Maps type declarations to the corresponding type objects to allow access to the implementations
   * of super classes.
   */
  private final Map<TypeDeclaration, Type> typesByTypeDeclaration = new HashMap<>();

  public WasmModuleGenerator(Path outputPath, Problems problems) {
    this.outputPath = outputPath;
    this.problems = problems;
  }

  public void generateOutputs(List<CompilationUnit> j2clCompilationUnits) {
    // Collect the implementations for all types
    j2clCompilationUnits.stream()
        .flatMap(cu -> cu.getTypes().stream())
        .forEach(
            type -> checkState(typesByTypeDeclaration.put(type.getDeclaration(), type) == null));
    builder.append("(module");
    builder.indent();
    for (CompilationUnit j2clCompilationUnit : j2clCompilationUnits) {
      builder.newLine();
      builder.append(
          ";;; " + j2clCompilationUnit.getPackageName() + "." + j2clCompilationUnit.getName());
      for (Type type : j2clCompilationUnit.getTypes()) {
        renderType(type);
      }
    }
    builder.unindent();
    builder.newLine();
    builder.append(")");

    J2clUtils.writeToFile(outputPath.resolve("module.wat"), builder.build(), problems);
  }

  private void renderType(Type type) {
    renderTypeStruct(type);
  }

  private void renderTypeStruct(Type type) {
    builder.newLine();
    builder.append("(type $" + type.getDeclaration().getMangledName() + " (struct ");
    builder.indent();
    renderTypeFields(type);
    builder.unindent();
    builder.newLine();
    builder.append("))");
  }

  private void renderTypeFields(Type type) {
    DeclaredTypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor != null) {
      Type supertype = typesByTypeDeclaration.get(superTypeDescriptor.getTypeDeclaration());
      if (supertype == null) {
        builder.newLine();
        builder.append(";; Missing superttype " + superTypeDescriptor.getReadableDescription());
      } else {
        renderTypeFields(typesByTypeDeclaration.get(superTypeDescriptor.getTypeDeclaration()));
      }
    }
    for (Field field : type.getFields()) {
      if (field.isStatic()) {
        continue;
      }
      builder.newLine();
      builder.append(
          "(field "
              + getFieldName(field)
              + " "
              + getWasmType(field.getDescriptor().getTypeDescriptor())
              + ")");
    }
  }

  private static String getFieldName(Field field) {
    return "$" + field.getDescriptor().getMangledName();
  }

  private static String getWasmType(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isPrimitive()) {
      return WASM_TYPES_BY_PRIMITIVE_TYPES.get(typeDescriptor);
    }
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      return "(ref null $" + ((DeclaredTypeDescriptor) typeDescriptor).getMangledName() + ")";
    }
    throw new AssertionError("Unimplemented type: " + typeDescriptor);
  }
}
