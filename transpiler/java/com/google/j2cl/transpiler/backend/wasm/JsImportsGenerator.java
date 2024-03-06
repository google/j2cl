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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/** Generates a JavaScript imports mapping for the Wasm module. */
public final class JsImportsGenerator {

  /** Top-level module name in the imports map containing all generated imports. */
  public static final String MODULE = "imports";

  /** Represents the JavaScript imports for a the Wasm module. */
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

  /** Collects JavaScript imports that are referenced in the library. */
  public static Imports collectImports(Library library, Problems problems) {
    ImmutableSet.Builder<String> moduleImports = ImmutableSet.builder();
    ImmutableMap.Builder<MethodDescriptor, JsMethodImport> methodImports = ImmutableMap.builder();
    library.accept(new ImportCollector(problems, methodImports, moduleImports));
    return Imports.create(methodImports.buildOrThrow(), moduleImports.build());
  }

  /** Generates the JavaScript code to support the imports. */
  public static void generateOutputs(Output output, Imports imports) {
    JsImportsGenerator importsGenerator = new JsImportsGenerator(imports);
    output.write(
        "imports.txt",
        generateOutputs(
            importsGenerator.imports.getModuleImports(),
            imports.getMethodImports().values().stream()
                .collect(
                    toImmutableMap(
                        JsMethodImport::getImportKey,
                        importsGenerator::createImportBody,
                        (i1, i2) -> i1))));
  }

  /** Generates the JavaScript code to support the imports. */
  public static String generateOutputs(
      Collection<String> requiredModules, Map<String, String> methodImports) {
    SourceBuilder builder = new SourceBuilder();
    emitRequires(builder, requiredModules);
    emitJsImports(builder, methodImports);
    builder.newLine(); // Ends in a new line for human readability.
    return builder.build();
  }

  /** Collects the import snippets indexed by their keys. */
  static Map<String, String> collectImportSnippets(Imports imports) {
    JsImportsGenerator importsGenerator = new JsImportsGenerator(imports);
    return imports.getMethodImports().values().stream()
        .distinct()
        .sorted(comparing(JsMethodImport::getImportKey))
        .collect(
            toImmutableMap(
                JsMethodImport::getImportKey, importsGenerator::createImportBody, (i1, i2) -> i1));
  }

  private static void emitRequires(SourceBuilder builder, Collection<String> requiredModules) {
    requiredModules.stream()
        .sorted()
        .forEach(
            i -> {
              builder.append(createGoogRequire(i));
              builder.newLine();
            });
  }

  private static String createGoogRequire(String importedModule) {
    return String.format(
        "const %s = goog.require('%s');",
        JsMethodImport.computeJsAlias(importedModule), importedModule);
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
  private static void emitJsImports(SourceBuilder builder, Map<String, String> methodImports) {
    builder.newLine();
    builder.append("/** @return {!Object<string, *>} Wasm import object */");
    builder.newLine();
    builder.append("function getImports() ");
    builder.openBrace();
    builder.newLine();
    builder.append("return ");
    builder.openBrace();
    builder.newLine();
    // Add WebAssembly module. This is needed because the import is hardcoded in
    // `generateWasmModule` and there is no corresponding code in the stb lib.
    // TODO(b/277970998): Consider how to handle this.
    builder.append("'WebAssembly': WebAssembly,");
    builder.newLine();
    builder.append(String.format("'%s': ", MODULE));
    builder.openBrace();
    methodImports.entrySet().stream()
        .sorted(Entry.comparingByKey())
        .forEach(
            i -> {
              builder.newLine();
              builder.append(String.format("'%s': %s", i.getKey(), i.getValue()));
              builder.append(",");
            });
    builder.closeBrace();
    builder.closeBrace();
    builder.append(";");
    builder.closeBrace();
  }

  private String createImportBody(JsMethodImport methodImport) {
    if (methodImport.emitAsMethodReference()) {
      return AstUtils.buildQualifiedName(methodImport.getJsQualifier(), methodImport.getJsName());
    }

    return createLambdaExpressionCode(methodImport);
  }

  private String createLambdaExpressionCode(JsMethodImport methodImport) {
    StringBuilder sb = new StringBuilder();
    // Emit parameters
    sb.append("(");
    if (methodImport.isInstance()) {
      sb.append(
          createParameterDefinition(
              new Variable.Builder()
                  .setName("$instance")
                  .setTypeDescriptor(
                      methodImport
                          .getMethod()
                          .getDescriptor()
                          .getEnclosingTypeDescriptor()
                          .toNonNullable())
                  .build()));
    }
    methodImport.getParameters().forEach(v -> sb.append(createParameterDefinition(v)));
    sb.append(") => ");

    // Emit function name
    if (methodImport.isConstructor()) {
      sb.append(String.format("new %s", methodImport.getJsQualifier()));
    } else if (methodImport.isInstance()) {
      sb.append(String.format("$instance.%s", methodImport.getJsName()));
    } else {
      sb.append(
          AstUtils.buildQualifiedName(methodImport.getJsQualifier(), methodImport.getJsName()));
    }

    // Emit arguments
    if (methodImport.isPropertyGetter()) {
      return sb.toString();
    }
    if (methodImport.isPropertySetter()) {
      sb.append(" = ");
      sb.append(methodImport.getParameters().get(0).getName());
      return sb.toString();
    }
    sb.append("(");
    for (var parameter : methodImport.getParameters()) {
      sb.append(parameter.getName() + ", ");
    }
    sb.append(")");
    return sb.toString();
  }

  private String createParameterDefinition(Variable parameter) {
    return String.format(
        "/** %s */ %s, ",
        // TODO(b/285407647): Make nullability consistent for parameterized types, etc.
        closureEnvironment.getClosureTypeString(parameter.getTypeDescriptor().toNonNullable()),
        parameter.getName());
  }

  private static class ImportCollector extends AbstractVisitor {
    private final Problems problems;
    private final ImmutableMap.Builder<MethodDescriptor, JsMethodImport> methodImports;
    private final ImmutableSet.Builder<String> moduleImports;
    private final Map<String, JsMethodImport> methodImportsByName = new HashMap<>();
    private final ClosureGenerationEnvironment closureEnvironment =
        createNominalClosureEnvironment();

    public ImportCollector(
        Problems problems,
        ImmutableMap.Builder<MethodDescriptor, JsMethodImport> methodImports,
        ImmutableSet.Builder<String> moduleImports) {
      this.problems = problems;
      this.methodImports = methodImports;
      this.moduleImports = moduleImports;
    }

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
              .setSignature(JsMethodImport.computeSignature(methodDescriptor, closureEnvironment))
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

                checkForConflicts(newImport, existingImport, problems);
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
  }

  private static void checkForConflicts(
      JsMethodImport newImport, JsMethodImport existingImport, Problems problems) {
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

    if (!newImport.getSignature().equals(existingImport.getSignature())
        // Signature doesn't matter if the method import is emitted as a method
        // reference. Exclude these in this check.
        && !(newImport.emitAsMethodReference() && existingImport.emitAsMethodReference())) {
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

  /** Creates a minimal closure generation environment to reuse {@code ClosureTypesGenerator}. */
  private static ClosureGenerationEnvironment createNominalClosureEnvironment() {
    return new ClosureGenerationEnvironment(ImmutableSet.of(), ImmutableMap.of()) {
      @Override
      public String aliasForType(TypeDeclaration typeDeclaration) {
        return JsMethodImport.getJsTypeName(typeDeclaration);
      }
    };
  }

  private final Imports imports;

  /** A minimal closure generation environment to reuse {@code ClosureTypesGenerator}. */
  private final ClosureGenerationEnvironment closureEnvironment = createNominalClosureEnvironment();

  private JsImportsGenerator(Imports imports) {
    this.imports = imports;
  }
}
