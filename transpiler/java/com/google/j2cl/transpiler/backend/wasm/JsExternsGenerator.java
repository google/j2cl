/*
 * Copyright 2025 Google Inc.
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

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.toOptional;
import static com.google.j2cl.transpiler.ast.AstUtils.isWasmJsExportedType;
import static java.util.stream.Collectors.joining;

import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.JsMemberType;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Generates JavaScript externs for allowing JavaScript callers to use exported JsTypes.
 *
 * <p>This is to provide typing information, for types and functions, for the exported JsTypes to
 * JavaScript callers. The actual concrete types are created at runtime by Wasm.
 */
final class JsExternsGenerator {

  private static final String OUTPUT_PATH = "externs";

  /** The Closure modules that define the native types referenced by the extern. */
  private final Set<String> referencedNativeModules = new LinkedHashSet<>();

  private final WasmJsInteropClosureGenerationEnvironment closureEnvironment =
      new WasmJsInteropClosureGenerationEnvironment() {
        @Override
        public String aliasForType(TypeDeclaration typeDeclaration) {
          if (typeDeclaration.isExtern() || typeDeclaration.isNative()) {
            // In extern files, externs and native types can be referenced using their Javascript
            // qualified name.
            if (!typeDeclaration.isExtern()) {
              // Native types are defined in Closure modules that are not necessarily
              // goog.require'd by the sources of the library, record them so that they can be
              // goog.requireType'd from the externs wiring. Otherwise the pruning in the
              // library level type checking might drop them and the types would be unknown.
              referencedNativeModules.add(
                  typeDeclaration.getEnclosingModule().getQualifiedJsName());
            }
            return typeDeclaration.getQualifiedJsName();
          }
          // Wasm types, on the other hand, need a different alias to be used in their externs
          // definitions.
          return "$j2wasm$externs_" + super.aliasForType(typeDeclaration);
        }
      };

  private final Output output;
  private final Type type;

  private JsExternsGenerator(Output output, Type type) {
    this.output = output;
    this.type = type;
  }

  /** Generates the JavaScript code to support the imports. */
  public static void generateOutputs(
      Output output, WasmGenerationEnvironment environment, Library library) {
    if (!environment.isCustomDescriptorsJsInteropEnabled()) {
      return;
    }
    library
        .streamTypes()
        .filter(t -> shouldGenerateExtern(t.getTypeDescriptor()))
        .forEach(t -> generateExterns(output, t));
  }

  private static void generateExterns(Output output, Type type) {
    var externsGenerator = new JsExternsGenerator(output, type);
    // Generate the extern first since it records the native modules that need to be required in
    // the wiring.
    externsGenerator.generateExtern();
    externsGenerator.generateExternsWiring();
  }

  static boolean shouldGenerateExtern(DeclaredTypeDescriptor typeDescriptor) {
    // Generate externs if this type is visible to JS.
    return isWasmJsExportedType(typeDescriptor);
  }

  private void generateExtern() {
    SourceBuilder sb = new SourceBuilder();
    sb.appendln("/** @externs */");

    appendConstructor(sb);
    appendMembers(sb);

    // Output to externs/my.package.MyClass.externs.java.js
    output.write(
        Path.of(OUTPUT_PATH, type.getDeclaration().getQualifiedJsName() + ".externs.java.js")
            .toString(),
        sb.build());
  }

  /** Appends the constructor extern for the type. */
  private void appendConstructor(SourceBuilder sb) {
    // Retrieve the synthetic factory method for the constructor, if it exists, since the
    // normalization removes the original constructor.
    Method factoryMethod =
        type.getMethods().stream()
            .filter(
                m ->
                    m.getDescriptor().getOrigin()
                            == MethodDescriptor.MethodOrigin.SYNTHETIC_FACTORY_FOR_CONSTRUCTOR
                        && m.getDescriptor().canBeReferencedExternallyForWasm())
            .collect(toOptional())
            .orElse(null);

    String jsDoc =
        closureEnvironment.getJsDocForType(
            type.toBuilder()
                // If the constructor was not exported, also declare the type as abstract since
                // it cannot be instantiated.
                .setAbstract(factoryMethod == null || type.isAbstract())
                .build());
    sb.appendln("");
    if (type.isJsEnum()) {
      // Emit an interface so that the value type for the JsEnum is opaque.
      sb.appendln("/**");
      sb.appendln(" * @interface");
      sb.appendln(" */");
      sb.appendln(
          String.format(
              "var %s = function() {};",
              closureEnvironment.getJsEnumValueType(type.getDeclaration())));

      sb.appendln("");
      sb.appendln("/**");
      appendJsDoc(sb, jsDoc);
      sb.appendln(" */");
      sb.appendln(
          String.format("var %s = {};", closureEnvironment.aliasForType(type.getDeclaration())));
      return;
    }
    sb.appendln("/**");
    appendJsDoc(sb, jsDoc);
    if (!type.isInterface()) {
      sb.appendln(" * @constructor");
    }
    sb.appendln(" */");
    sb.append(
        String.format("var %s = function", closureEnvironment.aliasForType(type.getDeclaration())));

    closureEnvironment.emitParameters(
        sb, factoryMethod != null ? factoryMethod : getEmptyConstructorStub(type));
    sb.appendln("{};");
  }

  /** Creates an empty constructor stub to be used when the constructor is not exported to JS. */
  private static Method getEmptyConstructorStub(Type type) {
    return Method.builder()
        .setMethodDescriptor(AstUtils.createImplicitConstructorDescriptor(type.getTypeDescriptor()))
        .setSourcePosition(SourcePosition.NONE)
        .build();
  }

  private void appendMembers(SourceBuilder sb) {
    Stream.concat(streamExportedMembers(type), streamGetterSetterPairsAsFields(type))
        .forEach(
            member -> {
              switch (member) {
                case FieldDescriptor f -> appendField(sb, f);
                // Skip JsProperty methods since they are handled as fields above and skip
                // JsConstructor methods since they have special handling.
                case MethodDescriptor m when m.isJsMethod() -> appendMethod(sb, m);
                default -> {}
              }
            });
  }

  private void appendField(SourceBuilder sb, FieldDescriptor fieldDescriptor) {
    var propertyJsDoc = closureEnvironment.getJsDocForField(fieldDescriptor);
    var propertyQualifiedName = getMemberProperty(fieldDescriptor);
    sb.append(
        """

        /** %s */
        %s;
        """
            .formatted(propertyJsDoc, propertyQualifiedName));
  }

  private void appendMethod(SourceBuilder sb, MethodDescriptor methodDescriptor) {
    var method = createMethodStub(methodDescriptor);
    sb.newLine();
    sb.appendln("/**");
    String jsDoc = closureEnvironment.getJsDocForMethod(method);
    appendJsDoc(sb, jsDoc);
    sb.appendln(" */");
    sb.append(String.format("%s = function", getMemberProperty(methodDescriptor)));
    closureEnvironment.emitParameters(sb, method);
    sb.appendln("{};");
  }

  /** Streams all exported methods for the given type. */
  private static Stream<MemberDescriptor> streamExportedMembers(Type type) {
    // Stream members from the type declaration since some members are not present in the Type
    // object (e.g. compile time constant fields)
    return Stream.concat(
            type.getDeclaration().getDeclaredMethodDescriptors().stream(),
            type.getDeclaration().getDeclaredFieldDescriptors().stream())
        .filter(not(MemberDescriptor::isSynthetic))
        .filter(MemberDescriptor::canBeReferencedExternallyForWasm);
  }

  /** Collates getter/setter pairs and presents them as a field descriptor. */
  private static Stream<MemberDescriptor> streamGetterSetterPairsAsFields(Type type) {
    record PropertyKey(String name, boolean isStatic) {}

    var getterSetters = new LinkedHashMap<PropertyKey, GetterSetterPair>();
    streamExportedMembers(type)
        .filter(MemberDescriptor::isJsProperty)
        .filter(MethodDescriptor.class::isInstance)
        .map(MethodDescriptor.class::cast)
        .forEach(
            m -> {
              GetterSetterPair getterSetterPair =
                  getterSetters.computeIfAbsent(
                      new PropertyKey(m.getSimpleJsName(), m.isStatic()),
                      k -> new GetterSetterPair());
              if (m.isJsPropertyGetter()) {
                getterSetterPair.getter = m;
              } else if (m.isJsPropertySetter()) {
                getterSetterPair.setter = m;
              }
            });
    return getterSetters.values().stream().map(GetterSetterPair::asFieldDescriptor);
  }

  private static class GetterSetterPair {
    private MethodDescriptor getter = null;
    private MethodDescriptor setter = null;

    /**
     * Returns a field descriptor representing the getter/setter pair which can be used to generate
     * the extern as if it were a field.
     */
    FieldDescriptor asFieldDescriptor() {
      MethodDescriptor primary = getter != null ? getter : setter;
      TypeDescriptor typeDescriptor =
          getter != null
              ? getter.getReturnTypeDescriptor()
              : setter.getParameterTypeDescriptors().get(0);
      return FieldDescriptor.builder()
          .setEnclosingTypeDescriptor(primary.getEnclosingTypeDescriptor())
          .setName(primary.getSimpleJsName())
          .setTypeDescriptor(typeDescriptor)
          .setStatic(primary.isStatic())
          // If there is no setter, mark as compile time constant. It's not technically a compile
          // time constant, but this is needed to emit @const which is used for constants and
          // read-only properties.
          .setFinal(setter == null)
          .setOriginalJsInfo(
              primary.getJsInfo().toBuilder()
                  .setJsName(primary.getSimpleJsName())
                  .setJsMemberType(JsMemberType.PROPERTY)
                  .build())
          .build();
    }
  }

  /**
   * Creates a Method stub to reuse the logic in ClosureTypesGenerator to emit JSDoc.
   *
   * <p>Note: This is needed because the method descriptors need to be modified to emit correct
   * externs (e.g. removing varargs, removing abstract, etc).
   */
  private static Method createMethodStub(MethodDescriptor methodDescriptor) {
    return Method.builder()
        .setMethodDescriptor(
            methodDescriptor.toBuilder()
                // Remove varargs since methods exposed through configureAll are never varargs.
                .setParameterDescriptors(
                    methodDescriptor.getParameterDescriptors().stream()
                        .map(parameter -> parameter.toBuilder().setVarargs(false).build())
                        .collect(toImmutableList()))
                // Methods declared in externs should not be declared as abstract since we are only
                // defining the JavaScript contract.
                .setAbstract(false)
                .build())
        .setParameters(
            AstUtils.createParameterVariables(methodDescriptor.getParameterDescriptors()))
        .setSourcePosition(SourcePosition.NONE)
        .build();
  }

  private String getMemberProperty(MemberDescriptor memberDescriptor) {
    return String.format(
        "%s%s.%s",
        closureEnvironment.aliasForType(memberDescriptor.getEnclosingTypeDescriptor()),
        memberDescriptor.isStatic() ? "" : ".prototype",
        memberDescriptor.getSimpleJsName());
  }

  /** Renders a JsDoc clause. The provided jsdoc can be single line or multi-line. */
  private void appendJsDoc(SourceBuilder sb, String jsDoc) {
    if (jsDoc.isEmpty()) {
      return;
    }
    if (jsDoc.endsWith("\n")) {
      sb.append(jsDoc);
    } else {
      // Single line jsdoc - turn it into a multi-line jsdoc by prepending a "*" and appending a
      // newline.
      sb.appendln(" *" + jsDoc);
    }
  }

  private void generateExternsWiring() {
    // Output to externs/my.package.MyClass.java.js
    output.write(
        Path.of(OUTPUT_PATH, type.getDeclaration().getQualifiedJsName() + ".java.js").toString(),
        getExternsWiringContent());
  }

  private String getExternsWiringContent() {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    String externName = closureEnvironment.aliasForType(typeDeclaration);
    String simpleJsName = typeDeclaration.getSimpleJsName();
    String qualifiedJsName = typeDeclaration.getQualifiedJsName();
    String moduleName = typeDeclaration.getModuleName();
    String requireTypes = getRequireTypes(moduleName);
    if (type.getMembers().stream().anyMatch(AstUtils::isExposedToJsViaConstructor)) {
      return generateConstructorProxy(
          moduleName, requireTypes, externName, simpleJsName, qualifiedJsName);
    } else {
      return generateTypeAlias(moduleName, requireTypes, externName, simpleJsName);
    }
  }

  /**
   * Returns the goog.requireType statements for the modules that define the native types referenced
   * from the externs.
   */
  private String getRequireTypes(String moduleName) {
    var requires =
        referencedNativeModules.stream()
            .filter(not(moduleName::equals))
            .map("goog.requireType('%s');"::formatted)
            .collect(joining("\n"));
    return requires.isEmpty() ? "" : requires + "\n";
  }

  private static String generateConstructorProxy(
      String moduleName,
      String requireTypes,
      String externName,
      String simpleJsName,
      String qualifiedJsName) {
    return """
    goog.module('%1$s');

    const {constructorProxy} = goog.require('j2wasm.JsInteropRuntime');
    %2$s
    /** @const {typeof %3$s} */
    const %4$s = constructorProxy('%5$s');

    exports = %4$s;
    """
        .formatted(moduleName, requireTypes, externName, simpleJsName, qualifiedJsName);
  }

  private static String generateTypeAlias(
      String moduleName, String requireTypes, String externName, String simpleJsName) {
    return """
    goog.module('%1$s');
    %2$s
    /** @typedef {%3$s} */
    let %4$s;

    exports = %4$s;
    """
        .formatted(moduleName, requireTypes, externName, simpleJsName);
  }
}
