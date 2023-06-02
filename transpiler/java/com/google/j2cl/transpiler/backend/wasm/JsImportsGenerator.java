/*
 * Copyright 2023 Google Inc.
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

import static java.util.Comparator.comparing;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.backend.closure.ClosureGenerationEnvironment;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.HashMap;
import java.util.Map;

/** Generates a JavaScript imports mapping for the Wasm module. */
final class JsImportsGenerator {

  /** Top-level module name in the imports map containing all generated imports. */
  public static final String MODULE = "imports";

  public static Imports collectImports(Library library, Problems problems) {
    return new JsImportsGenerator(problems).collectJsImports(library);
  }

  @AutoValue
  public abstract static class Imports {
    public abstract ImmutableMap<MethodDescriptor, JsMethodImport> getMethodImports();

    public abstract ImmutableSet<String> getModuleImports();

    public static Imports create(
        ImmutableMap<MethodDescriptor, JsMethodImport> methodImports,
        ImmutableSet<String> moduleImports) {
      return new AutoValue_JsImportsGenerator_Imports(methodImports, moduleImports);
    }
  }

  private final Output output;
  private final SourceBuilder builder = new SourceBuilder();

  private final WasmGenerationEnvironment environment;
  private final Problems problems;

  /** A minimal closure generation environment to reuse {@code ClosureTypesGenerator}. */
  private final ClosureGenerationEnvironment closureEnvironment =
      new ClosureGenerationEnvironment(ImmutableSet.of(), ImmutableMap.of()) {
        @Override
        public String aliasForType(TypeDeclaration typeDeclaration) {
          return JsMethodImport.getJsTypeName(typeDeclaration);
        }
      };

  public JsImportsGenerator(
      Output output, WasmGenerationEnvironment environment, Problems problems) {
    this.output = output;
    this.environment = environment;
    this.problems = problems;
  }

  private JsImportsGenerator(Problems problems) {
    this.output = null;
    this.environment = null;
    this.problems = problems;
  }

  public void generateOutputs(Library library) {
    emitRequires();
    emitJsImports();
    builder.newLine(); // Ends in a new line for human readability.
    output.write("imports.txt", builder.build());
  }

  private void emitRequires() {
    environment.getJsImports().getModuleImports().stream()
        .sorted()
        .forEach(
            imp -> {
              builder.append(
                  String.format(
                      "const %s = goog.require('%s');", JsMethodImport.computeJsAlias(imp), imp));
              builder.newLine();
            });
  }

  /**
   * Emits the function returning the import object. The import object is a nested {@code Object} in
   * the form:
   *
   * <pre>{@code
   * return {
   *   'imports': {
   *     'functionName': () => functionName(),
   *   }
   * }
   * }</pre>
   */
  private void emitJsImports() {
    builder.newLine();
    builder.append("/** @return {!Object<string, !Object<string, !*>>} Wasm import object */");
    builder.newLine();
    builder.append("function getImports() ");
    builder.openBrace();
    builder.newLine();
    builder.append("return ");
    builder.openBrace();
    builder.newLine();
    builder.append(String.format("'%s': ", MODULE));
    builder.openBrace();
    emitHardCodedJsImports();
    environment.getJsImports().getMethodImports().values().stream()
        .distinct()
        .sorted(comparing(JsMethodImport::getImportKey))
        .forEach(
            imp -> {
              builder.newLine();
              builder.append(String.format("'%s': ", imp.getImportKey()));
              emitImportBody(imp);
              builder.append(",");
            });
    builder.closeBrace();
    builder.closeBrace();
    builder.append(";");
    builder.closeBrace();
  }

  private void emitHardCodedJsImports() {
    // Add j2wasm.ExceptionUtils.tag. This is needed because the import is hardcoded in
    // `generateWasmModule` and there is no corresponding code in the stb lib.
    // TODO(b/277970998): Consider how to handle this.
    builder.newLine();
    builder.append("'j2wasm.ExceptionUtils.tag': j2wasm_ExceptionUtils.tag,");
  }

  private void emitImportBody(JsMethodImport methodImport) {
    if (methodImport.emitAsMethodReference()) {
      builder.append(
          AstUtils.buildQualifiedName(methodImport.getJsQualifier(), methodImport.getJsName()));
      return;
    }

    emitLambdaExpression(methodImport);
  }

  private void emitLambdaExpression(JsMethodImport methodImport) {
    // Emit parameters
    builder.append("(");
    if (methodImport.isInstance()) {
      emitParameter(
          new Variable.Builder()
              .setName("$instance")
              .setTypeDescriptor(
                  methodImport
                      .getMethod()
                      .getDescriptor()
                      .getEnclosingTypeDescriptor()
                      .toNonNullable())
              .build());
    }
    methodImport.getParameters().forEach(this::emitParameter);
    builder.append(") => ");

    // Emit function name
    if (methodImport.isConstructor()) {
      builder.append(String.format("new %s", methodImport.getJsQualifier()));
    } else if (methodImport.isInstance()) {
      builder.append(String.format("$instance.%s", methodImport.getJsName()));
    } else {
      builder.append(
          AstUtils.buildQualifiedName(methodImport.getJsQualifier(), methodImport.getJsName()));
    }

    // Emit arguments
    if (methodImport.isPropertyGetter()) {
      return;
    }
    if (methodImport.isPropertySetter()) {
      builder.append(" = ");
      builder.append(methodImport.getParameters().get(0).getName());
      return;
    }
    builder.append("(");
    for (var parameter : methodImport.getParameters()) {
      builder.append(parameter.getName() + ", ");
    }
    builder.append(")");
  }

  private void emitParameter(Variable parameter) {
    builder.append(
        String.format(
            "/** %s */ %s, ",
            // TODO(b/285407647): Make nullability consistent for parameterized types, etc.
            closureEnvironment.getClosureTypeString(parameter.getTypeDescriptor().toNonNullable()),
            parameter.getName()));
  }

  private Imports collectJsImports(Library library) {
    ImmutableSet.Builder<String> moduleImports = ImmutableSet.builder();
    Map<String, JsMethodImport> methodImportsByName = new HashMap<>();
    ImmutableMap.Builder<MethodDescriptor, JsMethodImport> methodImports = ImmutableMap.builder();
    library.accept(
        new AbstractVisitor() {
          @Override
          public void exitType(Type type) {
            collectModuleImports(type.getTypeDescriptor());
          }

          @Override
          public void exitMethod(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();
            if (!shouldGenerateImport(methodDescriptor)) {
              return;
            }

            addMethodImport(
                JsMethodImport.newBuilder()
                    .setBaseImportKey(JsMethodImport.getJsImportName(methodDescriptor))
                    .setSignature(
                        JsMethodImport.computeSignature(methodDescriptor, closureEnvironment))
                    .setMethod(method)
                    .build());

            // Collect imports for JsDoc.
            addModuleImports(methodDescriptor);
          }

          private void addMethodImport(JsMethodImport newImport) {
            JsMethodImport newOrExistingImport =
                methodImportsByName.compute(
                    newImport.getImportKey(),
                    (keyUnused, existingImport) -> {
                      if (existingImport == null) {
                        return newImport;
                      }

                      if (!JsMethodImport.isCompatible(existingImport, newImport)) {
                        problems.error(
                            newImport.getMethod().getSourcePosition(),
                            "Native methods '%s' and '%s', importing JavaScript method '%s', do not"
                                + " match. Both or neither must be constructors, static, or JS"
                                + " properties (b/283986050).",
                            existingImport.getMethod().getReadableDescription(),
                            newImport.getMethod().getReadableDescription(),
                            existingImport.getImportKey());
                      }

                      if (!newImport.getSignature().equals(existingImport.getSignature())) {
                        problems.error(
                            newImport.getMethod().getSourcePosition(),
                            "Native methods '%s' and '%s', importing JavaScript method '%s', have"
                                + " different parameter types ('%s' vs '%s'), currently disallowed"
                                + " due to performance concerns (b/279081023).",
                            existingImport.getMethod().getReadableDescription(),
                            newImport.getMethod().getReadableDescription(),
                            existingImport.getImportKey(),
                            existingImport.getSignature(),
                            newImport.getSignature());
                      }
                      return existingImport;
                    });
            methodImports.put(newImport.getMethod().getDescriptor(), newOrExistingImport);
          }

          private void addModuleImports(MethodDescriptor methodDescriptor) {
            if (!methodDescriptor.isExtern()) {
              moduleImports.add(methodDescriptor.getJsNamespace());
            }

            methodDescriptor.getParameterTypeDescriptors().forEach(this::collectModuleImports);
          }

          private void collectModuleImports(TypeDescriptor typeDescriptor) {
            if (!(typeDescriptor instanceof DeclaredTypeDescriptor)) {
              return;
            }
            DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
            TypeDeclaration typeDeclaration = declaredTypeDescriptor.getTypeDeclaration();
            if (!typeDeclaration.isNative() || typeDeclaration.isExtern()) {
              return;
            }
            moduleImports.add(typeDeclaration.getEnclosingModule().getQualifiedJsName());
            for (TypeDescriptor t : declaredTypeDescriptor.getTypeArgumentDescriptors()) {
              collectModuleImports(t);
            }
          }
        });
    return Imports.create(methodImports.buildOrThrow(), moduleImports.build());
  }

  private static boolean shouldGenerateImport(MethodDescriptor methodDescriptor) {
    if (!isNativeMethod(methodDescriptor)) {
      return false;
    }
    // If the method maps to a WASM instruction, that takes precedence.
    if (methodDescriptor.getWasmInfo() != null) {
      return false;
    }
    // Exclude private, parameterless constructors.
    // TODO(b/279187295) Make this more robust by checking for callers first.
    if (methodDescriptor.isConstructor()
        && methodDescriptor.getVisibility().isPrivate()
        && methodDescriptor.getParameterDescriptors().isEmpty()) {
      return false;
    }
    return true;
  }

  private static boolean isNativeMethod(MethodDescriptor methodDescriptor) {
    return methodDescriptor.isNative()
        // TODO(b/264676817): Consider refactoring to have MethodDescriptor.isNative return
        // true for native constructors, or exposing isNativeConstructor from
        // MethodDescriptor.
        || (methodDescriptor.getEnclosingTypeDescriptor().isNative()
            && methodDescriptor.isConstructor());
  }
}
