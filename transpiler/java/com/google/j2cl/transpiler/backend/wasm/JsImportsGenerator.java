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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Generates a JavaScript imports mapping for the Wasm module. */
final class JsImportsGenerator {

  /** Top-level module name in the imports map containing all generated imports. */
  public static final String MODULE = "imports";

  /**
   * Gets the name of the JS import for the specified JS method. This is suitable for referencing
   * the function in Wasm.
   */
  public static String getJsImportName(MethodDescriptor methodDescriptor) {
    String qualifiedJsName = methodDescriptor.getQualifiedJsName();
    if (methodDescriptor.isConstructor()) {
      // TODO(b/235601426): This is required until all tests and client code are migrated to use
      // the generated imports. Remove once complete.
      qualifiedJsName = qualifiedJsName.replace("<init>", "constructor");
    } else if (methodDescriptor.isPropertyGetter()) {
      qualifiedJsName = "get " + qualifiedJsName;
    } else if (methodDescriptor.isPropertySetter()) {
      qualifiedJsName = "set " + qualifiedJsName;
    }
    return qualifiedJsName;
  }

  private final Problems problems;
  private final Output output;
  private final SourceBuilder builder = new SourceBuilder();

  private final Set<String> moduleImports = new LinkedHashSet<>();

  /** Methods keyed on the name of the import. */
  private final Map<String, JsMethodImport> imports = new LinkedHashMap<>();

  /** A minimal closure generation environment to reuse {@code ClosureTypesGenerator}. */
  private final ClosureGenerationEnvironment closureEnvironment =
      new ClosureGenerationEnvironment(ImmutableSet.of(), ImmutableMap.of()) {
        @Override
        public String aliasForType(TypeDeclaration typeDeclaration) {
          return getJsTypeName(typeDeclaration);
        }
      };

  public JsImportsGenerator(Output output, Problems problems) {
    this.output = output;
    this.problems = problems;
  }

  public void generateOutputs(Library library) {
    collectJsImports(library);
    emitRequires();
    emitJsImports();
    builder.newLine(); // Ends in a new line for human readability.
    output.write("imports.txt", builder.build());
  }

  private void collectJsImports(Library library) {
    library.accept(
        new AbstractVisitor() {
          @Override
          public void exitType(Type type) {
            collectModuleImports(type.getTypeDescriptor());
          }

          @Override
          public void exitMethod(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();
            if (!isNativeMethod(methodDescriptor)) {
              return;
            }
            // If the method maps to a WASM instruction, that takes precedence.
            if (methodDescriptor.getWasmInfo() != null) {
              return;
            }
            // Skip when the enclosing type is "*" or similar, like `getTypeMarker` methods. This is
            // not supported in WASM.
            if (!methodDescriptor.isStatic()
                && methodDescriptor.getEnclosingTypeDescriptor().isStarOrUnknown()) {
              return;
            }
            // Exclude private, parameterless constructors.
            // TODO(b/279187295) Make this more robust by checking for callers first.
            if (methodDescriptor.isConstructor()
                && methodDescriptor.getVisibility().isPrivate()
                && methodDescriptor.getParameterDescriptors().isEmpty()) {
              return;
            }

            addModuleImports(methodDescriptor);

            mergeMethod(method);
          }

          private void addModuleImports(MethodDescriptor methodDescriptor) {
            if (!methodDescriptor.isExtern()) {
              moduleImports.add(methodDescriptor.getJsNamespace());
            }

            // Add parameters if they also need to be imported for JsDoc purposes.
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

          private void mergeMethod(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();
            String importKey = getJsImportName(methodDescriptor);
            JsMethodImport newImport = JsMethodImport.from(importKey, method, closureEnvironment);
            imports.compute(
                importKey,
                (key, existingImport) -> JsMethodImport.merge(existingImport, newImport, problems));
          }
        });
  }

  private void emitRequires() {
    for (String imp : moduleImports) {
      builder.append(String.format("const %s = goog.require('%s');", computeJsAlias(imp), imp));
      builder.newLine();
    }
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
    for (JsMethodImport m : imports.values()) {
      builder.newLine();
      builder.append(String.format("'%s': ", m.getImportKey()));
      emitImportBody(m);
      builder.append(",");
    }
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
    // As an optimization, if the method is static we can just assign the method reference EXCEPT
    // if it's a method on the Function prototype (e.g. `call`) or some other known exception.
    if (!methodImport.isInstance()
        && !methodImport.isConstructor()
        && !methodImport.isPropertyGetter()
        && !methodImport.isPropertySetter()
        // TODO(b/279080129) Figure out a more permanent solution for special cases here.
        && !methodImport.getJsName().equals("call")
        && !methodImport.getImportKey().equals("performance.now")) {
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
            closureEnvironment.getClosureTypeString(parameter.getTypeDescriptor()),
            parameter.getName()));
  }

  /** Information about a JavaScript method. */
  @AutoValue
  abstract static class JsMethodImport {
    public abstract String getImportKey();

    /**
     * A parameter signature, used to check if parameters are compatible with other imports of the
     * same method.
     */
    abstract String getSignature();

    /**
     * Qualifier for the JS method. For externs, this is the full qualifier--whatever appears before
     * final dot ('.') of the fully-qualified name. For non-externs, this is the alias to the JS
     * type.
     */
    @Memoized
    public String getJsQualifier() {
      MethodDescriptor methodDescriptor = getMethod().getDescriptor();
      if (methodDescriptor.isExtern()) {
        List<String> jsNameParts = splitQualifiedName(methodDescriptor.getQualifiedJsName());
        return jsNameParts.get(0);
      } else if (methodDescriptor.hasJsNamespace()) {
        return computeJsAlias(methodDescriptor.getJsNamespace());
      }
      return getJsTypeName(methodDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration());
    }

    /**
     * Short name for the JS method. For externs, this is the last part of the fully-qualified name,
     * whatever appears after the last dot ('.'). For non-externs, this is the simple JS name of the
     * method.
     */
    @Memoized
    public String getJsName() {
      MethodDescriptor methodDescriptor = getMethod().getDescriptor();
      if (methodDescriptor.isExtern()) {
        List<String> jsNameParts = splitQualifiedName(methodDescriptor.getQualifiedJsName());
        return jsNameParts.get(1);
      } else if (methodDescriptor.hasJsNamespace()) {
        return methodDescriptor.getSimpleJsName();
      }
      return methodDescriptor.getSimpleJsName();
    }

    /**
     * Splits the specified qualified name by the last dot ('.').
     *
     * @return a pair of (namespace, name).
     */
    private static ImmutableList<String> splitQualifiedName(String value) {
      int lastDot = value.lastIndexOf('.');
      return (lastDot == -1)
          ? ImmutableList.of("", value)
          : ImmutableList.of(value.substring(0, lastDot), value.substring(lastDot + 1));
    }

    public abstract Method getMethod();

    public boolean isConstructor() {
      return getMethod().getDescriptor().isConstructor();
    }

    public boolean isInstance() {
      return getMethod().getDescriptor().isInstanceMember();
    }

    public boolean isPropertyGetter() {
      return getMethod().getDescriptor().isPropertyGetter();
    }

    public boolean isPropertySetter() {
      return getMethod().getDescriptor().isPropertySetter();
    }

    public List<Variable> getParameters() {
      return getMethod().getParameters();
    }

    /**
     * Combines the parameter information of this method with the specified other method, and
     * verifies that the methods can share an import.
     *
     * <p>JavaScript functions that share the same import could have different parameter lists. For
     * example, {@code new Date(year, monthIndex)} and {@code new Date(year, monthIndex, day)}
     * merges into a {@code function(number, number, number)}.
     *
     * <p>We use a JavaScript rest parameter (varargs) when we encounter incompatible parameters.
     * For example, {@code new Date(dateString)} and {@code new Date(year, monthIndex)} merges into
     * {@code function(...*)}.
     */
    public static JsMethodImport merge(
        JsMethodImport existingImport, JsMethodImport newImport, Problems problems) {
      checkNotNull(newImport);
      if (existingImport == null) {
        return newImport;
      }

      // TODO(b/277803109) Consider moving the compatibility check to JsInteropRestrictionsChecker.
      if (!isCompatible(existingImport, newImport)) {
        problems.error(
            "Native method '%s' conflict, Existing: %s, New: %s",
            existingImport.getImportKey(),
            getDebugCompatibilityInfo(existingImport),
            getDebugCompatibilityInfo(newImport));
        return existingImport;
      }

      // Select which import to use. If parameters are compatible (one list is a prefix of the
      // other), we use the lengthier one.
      if (existingImport.getSignature().startsWith(newImport.getSignature())) {
        return existingImport;
      }
      if (newImport.getSignature().startsWith(existingImport.getSignature())) {
        return newImport;
      }

      // TODO(b/279081023) Support conflicting parameters by generating numbered imports.
      problems.error(
          "'%s' and '%s' import the same JavaScript method '%s' with conflicting parameter types,"
              + " currently disallowed due to performance concerns.",
          existingImport.getMethod().getReadableDescription(),
          newImport.getMethod().getReadableDescription(),
          existingImport.getImportKey());
      return existingImport;
    }

    private static boolean isCompatible(JsMethodImport existing, JsMethodImport other) {
      return existing.isConstructor() == other.isConstructor()
          && existing.isInstance() == other.isInstance()
          && existing.isPropertyGetter() == other.isPropertyGetter()
          && existing.isPropertySetter() == other.isPropertySetter();
    }

    private static String getDebugCompatibilityInfo(JsMethodImport methodImport) {
      return String.format(
          "Constructor=%s, Instance=%s, Getter=%s, Setter=%s",
          methodImport.isConstructor(),
          methodImport.isInstance(),
          methodImport.isPropertyGetter(),
          methodImport.isPropertySetter());
    }

    public static JsMethodImport from(
        String importKey, Method method, ClosureGenerationEnvironment closureEnvironment) {
      MethodDescriptor methodDescriptor = method.getDescriptor();
      checkArgument(isNativeMethod(methodDescriptor));

      return newBuilder()
          .setImportKey(importKey)
          .setSignature(computeSignature(methodDescriptor, closureEnvironment))
          .setMethod(method)
          .build();
    }

    /** Gets a signature for the native method using the native type of the parameters. */
    private static String computeSignature(
        MethodDescriptor methodDescriptor, ClosureGenerationEnvironment closureEnvironment) {
      return methodDescriptor.getParameterTypeDescriptors().stream()
          .map(closureEnvironment::getClosureTypeString)
          .collect(joining(","));
    }

    public static Builder newBuilder() {
      return new AutoValue_JsImportsGenerator_JsMethodImport.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setImportKey(String value);

      public abstract Builder setSignature(String value);

      public abstract Builder setMethod(Method value);

      public abstract JsMethodImport build();
    }
  }

  private static String getJsTypeName(TypeDeclaration typeDeclaration) {
    return AstUtils.buildQualifiedName(
        computeJsAlias(typeDeclaration.getEnclosingModule()),
        typeDeclaration.getInnerTypeQualifier());
  }

  private static String computeJsAlias(TypeDeclaration typeDeclaration) {
    if (typeDeclaration.isExtern()) {
      return typeDeclaration.getQualifiedJsName();
    }
    return computeJsAlias(typeDeclaration.getQualifiedJsName());
  }

  private static String computeJsAlias(String name) {
    return name.replace('.', '_');
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
