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
package com.google.j2cl.transpiler.backend.wasm;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableMap;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
  private Map<TypeDeclaration, Type> typesByTypeDeclaration;

  public WasmModuleGenerator(Path outputPath, Problems problems) {
    this.outputPath = outputPath;
    this.problems = problems;
  }

  public void generateOutputs(List<CompilationUnit> j2clCompilationUnits) {
    // Collect the implementations for all types
    typesByTypeDeclaration =
        j2clCompilationUnits.stream()
            .flatMap(cu -> cu.getTypes().stream())
            .collect(toImmutableMap(Type::getDeclaration, Function.identity()));
    builder.append("(module");
    builder.indent();
    for (CompilationUnit j2clCompilationUnit : j2clCompilationUnits) {
      builder.newLine();
      builder.append(
          ";;; Code for "
              + j2clCompilationUnit.getPackageName()
              + "."
              + j2clCompilationUnit.getName());
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
    renderTypeMethods(type);
  }

  private void renderTypeMethods(Type type) {
    for (Method method : type.getMethods()) {
      renderMethod(method);
    }
  }

  private void renderMethod(Method method) {
    builder.newLine();
    builder.append("(func " + getMethodImplementationName(method) + " ");
    // Emit parameters
    builder.indent();
    for (Variable parameter : method.getParameters()) {
      builder.newLine();
      builder.append(
          "(param $"
              + parameter.getName()
              + " "
              + getWasmType(parameter.getTypeDescriptor())
              + ")");
    }
    // Emit return type
    if (!TypeDescriptors.isPrimitiveVoid(method.getDescriptor().getReturnTypeDescriptor())) {
      builder.newLine();
      builder.append(
          "(result " + getWasmType(method.getDescriptor().getReturnTypeDescriptor()) + ")");
    }
    // Emit locals.
    // TODO(rluble): add variable collision resolver.
    for (Variable variable : collectLocals(method)) {
      builder.newLine();
      builder.append(
          "(local $" + variable.getName() + " " + getWasmType(variable.getTypeDescriptor()) + ") ");
    }
    builder.newLine();
    new StatementTranspiler(builder).renderStatement(method.getBody());
    builder.unindent();
    builder.newLine();
    builder.append(")");
  }

  /**
   * Returns the name of the global function that implements the method.
   *
   * <p>Note that this names need to be globally unique and this are different than the names of the
   * slots in the vtable which maps nicely to our concept of mangled names.
   */
  private static String getMethodImplementationName(Method method) {
    // TODO(b/171329507): review naming, the current mangled name concept used for JavaScript is not
    // enough since method names (in the implementation) need to be globally unique.
    // It is the fields in the vtables that can use the concept of mangled names to name the slots.
    return "$"
        + method.getDescriptor().getMangledName()
        + "__"
        + method.getDescriptor().getEnclosingTypeDescriptor().getMangledName();
  }

  private static List<Variable> collectLocals(Method method) {
    List<Variable> locals = new ArrayList<>();
    method.accept(
        new AbstractVisitor() {
          @Override
          public void exitVariableDeclarationFragment(VariableDeclarationFragment node) {
            locals.add(node.getVariable());
          }
        });
    return locals;
  }

  private void renderTypeStruct(Type type) {
    builder.newLine();
    builder.append("(type $" + type.getDeclaration().getMangledName() + " (struct");
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
        builder.append(";; Missing supertype " + superTypeDescriptor.getReadableDescription());
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
