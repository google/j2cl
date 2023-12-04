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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.not;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Generates all the outputs for Wasm compilation. */
public class WasmGeneratorStage {

  private final Problems problems;
  private final Output output;
  private final Path libraryInfoOutputPath;
  private WasmGenerationEnvironment environment;

  public WasmGeneratorStage(Output output, Path libraryInfoOutputPath, Problems problems) {
    this.output = output;
    this.libraryInfoOutputPath = libraryInfoOutputPath;
    this.problems = problems;
  }

  public void generateModularOutput(Library library) {
    if (libraryInfoOutputPath != null) {
      OutputUtils.writeToFile(libraryInfoOutputPath, new byte[0], problems);
    }

    environment =
        new WasmGenerationEnvironment(
            library, JsImportsGenerator.collectImports(library, problems), /* isModular= */ true);
    SummaryBuilder summaryBuilder = new SummaryBuilder(library, environment, problems);

    // TODO(rluble): Introduce/use flags to emit the readable version of the summary. For now emit
    // summaries in both binary and text form for now.
    output.write("summary.txtpb", summaryBuilder.toJson(problems));
    output.write("summary.binpb", summaryBuilder.toByteArray());

    List<ArrayTypeDescriptor> usedNativeArrayTypes = collectUsedNativeArrayTypes(library);

    copyJavaSources(library);

    emitToFile(
        "types.wat",
        generator -> {
          generator.emitDynamicDispatchMethodTypes();
          generator.emitNativeArrayTypes(usedNativeArrayTypes);
          generator.emitForEachType(
              library, generator::renderModularTypeStructs, "type definition");
        });

    emitToFile(
        "functions.wat",
        generator -> generator.emitForEachType(library, generator::renderTypeMethods, "methods"));

    emitToFile("globals.wat", generator -> generator.emitGlobals(library));

    emitToFile("data.wat", generator -> generator.emitDataSegments(library));

    generateJsImportsFile();
  }

  private void emitToFile(String filename, Consumer<WasmConstructsGenerator> emitter) {
    SourceBuilder builder = new SourceBuilder();
    WasmConstructsGenerator generator = new WasmConstructsGenerator(environment, builder);

    emitter.accept(generator);

    String content = builder.build();
      if (content.isEmpty()) {
      return;
    }
    output.write(filename, content);
  }

  public void generateMonolithicOutput(Library library) {
    copyJavaSources(library);
    generateWasmModule(library);
    generateJsImportsFile();
  }

  private void copyJavaSources(Library library) {
    library.getCompilationUnits().stream()
        .filter(not(CompilationUnit::isSynthetic))
        .forEach(
            compilationUnit ->
                output.copyFile(
                    compilationUnit.getFilePath(), compilationUnit.getPackageRelativePath()));
  }

  private void generateWasmModule(Library library) {
    environment =
        new WasmGenerationEnvironment(
            library, JsImportsGenerator.collectImports(library, problems));
    SourceBuilder builder = new SourceBuilder();
    WasmConstructsGenerator generator = new WasmConstructsGenerator(environment, builder);

    List<ArrayTypeDescriptor> usedNativeArrayTypes = collectUsedNativeArrayTypes(library);

    builder.appendln(";;; Code generated by J2WASM");
    builder.append("(module");
    // Emit all types at the beginning of the module.
    generator.emitLibraryRecGroup(library, usedNativeArrayTypes);

    generator.emitExceptionTag();

    // Emit all the globals, e.g. vtable instances, etc.
    generator.emitDataSegments(library);
    generator.emitDispatchTablesInitialization(library);
    generator.emitEmptyArraySingletons(usedNativeArrayTypes);
    generator.emitGlobals(library);

    // Emit intrinsics imports
    generator.emitImportsForBinaryenIntrinsics();

    // Last, emit all methods at the very end so that the synthetic code generated above does
    // not inherit an incorrect source position.
    generator.emitForEachType(library, generator::renderTypeMethods, "methods");

    builder.newLine();
    builder.append(")");
    output.write("module.wat", builder.buildToList());
    output.write("namemap", emitNameMapping(library));
  }

  public void generateMethods(List<Method> methods) {
    if (methods.isEmpty()) {
      return;
    }

    // Create the type objects and add all the exported methods to the corresponding type to
    // initialize the WasmGenerationEnvironment.
    CompilationUnit cu = CompilationUnit.createSynthetic("wasm.exports");
    Map<TypeDeclaration, Type> typesByDeclaration = new LinkedHashMap<>();
    methods.forEach(
        m -> {
          TypeDeclaration typeDeclaration =
              m.getDescriptor().getEnclosingTypeDescriptor().getTypeDeclaration();
          Type type =
              typesByDeclaration.computeIfAbsent(
                  typeDeclaration, t -> new Type(SourcePosition.NONE, t));
          type.addMember(m);
        });
    typesByDeclaration.values().forEach(cu::addType);
    Library library = Library.newBuilder().setCompilationUnits(ImmutableList.of(cu)).build();
    environment =
        new WasmGenerationEnvironment(
            library, JsImportsGenerator.collectImports(library, problems));

    SourceBuilder builder = new SourceBuilder();
    WasmConstructsGenerator generator = new WasmConstructsGenerator(environment, builder);

    methods.forEach(generator::renderMethod);
    output.write("functions.wat", builder.buildToList());
  }

  private List<ArrayTypeDescriptor> collectUsedNativeArrayTypes(Library library) {
    Set<ArrayTypeDescriptor> usedArrayTypes = new LinkedHashSet<>();
    // Collect native arrays from fields and variables; this covers all scenarios.
    library.accept(
        // TODO(b/303659726): Generalize a type visitor that could be used here and other places
        // like in ImportGatherer. Or consider emitting the one dimensional array type for all
        // native types.
        new AbstractVisitor() {
          @Override
          public void exitField(Field field) {
            collectIfArrayType(field.getDescriptor().getTypeDescriptor());
          }

          @Override
          public void exitVariable(Variable variable) {
            collectIfArrayType(variable.getTypeDescriptor());
          }

          private void collectIfArrayType(TypeDescriptor typeDescriptor) {
            if (!typeDescriptor.isArray()) {
              return;
            }

            ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
            if (arrayTypeDescriptor.isNativeWasmArray()) {
              usedArrayTypes.add(arrayTypeDescriptor);
            }
          }
        });

    return new ArrayList<>(usedArrayTypes);
  }

  private void generateJsImportsFile() {
    JsImportsGenerator.generateOutputs(output, environment.getJsImports());
  }

  private String emitNameMapping(Library library) {
    SourceBuilder builder = new SourceBuilder();
    library.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethod(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();
            String methodImplementationName =
                environment.getMethodImplementationName(methodDescriptor);
            checkState(methodImplementationName.startsWith("$"));
            builder.append(
                String.format(
                    "%s:%s",
                    methodImplementationName.substring(1),
                    methodDescriptor.getQualifiedBinaryName()));
            builder.newLine();
          }
        });
    return builder.build();
  }
}
