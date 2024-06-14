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
package com.google.j2cl.transpiler.backend.closure;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.j2cl.common.FilePosition;
import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.SourceLanguage;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.Visibility;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Generates JavaScript source impl files. */
public class JavaScriptImplGenerator extends JavaScriptGenerator {
  private NativeJavaScriptFile nativeSource;
  private final ClosureTypesGenerator closureTypesGenerator;

  public static final String FILE_SUFFIX = ".impl.java.js";

  public JavaScriptImplGenerator(Problems problems, Type type, List<Import> imports) {
    super(problems, type, imports);
    this.closureTypesGenerator = new ClosureTypesGenerator(environment);
  }

  private static String getMethodQualifiers(MethodDescriptor methodDescriptor) {
    String staticQualifier = methodDescriptor.isStatic() ? "static " : "";
    if (!methodDescriptor.isAbstract() && methodDescriptor.isJsAsync()) {
      // Do not emit the "async" modifier for abstract methods since jscompiler will emit
      // a warning due to the way async are transpiled down.
      return staticQualifier + "async ";
    }
    return staticQualifier;
  }

  /**
   * Emits the method header including (static) (async) (getter/setter) methodName(parametersList).
   */
  private void emitMethodHeader(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    sourceBuilder.append(getMethodQualifiers(methodDescriptor));
    sourceBuilder.emitWithMapping(
        method.getSourcePosition(), () -> sourceBuilder.append(methodDescriptor.getMangledName()));

    sourceBuilder.append("(");

    // TODO(goktug): Reuse with Expression transpiler which has similar logic.
    String separator = "";
    for (int i = 0; i < method.getParameters().size(); i++) {
      sourceBuilder.append(separator);
      // Emit parameters in the more readable inline short form.
      emitParameter(method, i);
      separator = ", ";
    }
    sourceBuilder.append(") ");
  }

  private void emitParameter(MethodLike expression, int i) {
    Variable parameter = expression.getParameters().get(i);

    if (parameter == expression.getJsVarargsParameter()) {
      sourceBuilder.append("...");
    }
    sourceBuilder.append(
        "/** " + closureTypesGenerator.getJsDocForParameter(expression, i) + " */ ");
    sourceBuilder.emitWithMapping(
        // Only map parameters if they are named.
        AstUtils.removeUnnamedSourcePosition(parameter.getSourcePosition()),
        () -> sourceBuilder.append(environment.getUniqueNameForVariable(parameter)));
  }

  @Override
  public String getSuffix() {
    return FILE_SUFFIX;
  }

  public void setNativeSource(NativeJavaScriptFile nativeSource) {
    this.nativeSource = checkNotNull(nativeSource);
  }

  @Override
  public String renderOutput() {
    try {
      renderImports();
      if (type.isJsEnum()) {
        renderClosureEnum();
      } else {
        renderClass();
      }
      renderExports();
      return sourceBuilder.build();
    } catch (RuntimeException e) {
      // Catch all unchecked exceptions and rethrow them with more context to make debugging easier.
      // Yes this is really being done on purpose.
      throw new InternalCompilerError(
          e,
          "Error generating source for type %s.",
          type.getDeclaration().getQualifiedBinaryName());
    }
  }

  private void renderClosureEnum() {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    sourceBuilder.append(
        "/** @enum {"
            + closureTypesGenerator.getClosureTypeString(
                AstUtils.getJsEnumValueFieldType(typeDeclaration))
            + "}");
    if (type.getDeclaration().isDeprecated()) {
      sourceBuilder.append(" @deprecated");
    }
    sourceBuilder.appendln(" */");
    sourceBuilder.append("const ");
    sourceBuilder.emitWithMapping(
        type.getSourcePosition(),
        () -> sourceBuilder.append(environment.aliasForType(typeDeclaration)));
    sourceBuilder.append(" = ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    for (Field field : type.getStaticFields()) {
      sourceBuilder.emitWithMemberMapping(
          field.getDescriptor(),
          () -> {
            if (field.getDescriptor().isDeprecated()) {
              sourceBuilder.appendln(" /** @deprecated */");
            }
            sourceBuilder.emitWithMapping(
                field.getSourcePosition(), () -> sourceBuilder.append(field.getMangledName()));
            sourceBuilder.append(" : ");
            ExpressionTranspiler.render(field.getInitializer(), environment, sourceBuilder);
            sourceBuilder.append(",");
            sourceBuilder.newLine();
          });
    }
    sourceBuilder.closeBrace();
    sourceBuilder.append(";");
    sourceBuilder.newLine();
  }

  public void renderClass() {
    renderTypeAnnotation();
    renderClassBody();
    renderLoadTimeStatements();
    renderNativeSource();
  }

  private void renderImports() {
    TypeDeclaration typeDeclaration = type.getDeclaration();

    // goog.module(...) declaration.
    sourceBuilder.appendln("goog.module('" + typeDeclaration.getImplModuleName() + "');");
    sourceBuilder.newLine();

    // goog.require(...) for eager imports.
    Map<String, String> aliasesByPath = new HashMap<>();
    sourceBuilder.emitBlock(
        imports.stream()
            .filter(i -> i.getImportCategory().needsGoogRequireInImpl())
            .collect(toImmutableList()),
        eagerImport -> {
          String alias = eagerImport.getAlias();
          String path = eagerImport.getImplModulePath();
          String previousAlias = aliasesByPath.get(path);
          if (previousAlias == null) {
            sourceBuilder.appendln("const " + alias + " = goog.require('" + path + "');");
            aliasesByPath.put(path, alias);
          } else {
            // Do not goog.require second time to avoid JsCompiler warnings.
            sourceBuilder.appendln("const " + alias + " = " + previousAlias + ";");
          }
        });

    // goog.forwardDeclare(...) for lazy imports.
    sourceBuilder.emitBlock(
        imports.stream()
            .filter(i -> i.getImportCategory().needsGoogForwardDeclare())
            .collect(toImmutableList()),
        lazyImport -> {
          String alias = lazyImport.getAlias();
          String path = lazyImport.getImplModulePath();
          sourceBuilder.appendln("let " + alias + " = goog.forwardDeclare('" + path + "');");
        });
  }

  private void renderTypeAnnotation() {
    if (type.isOverlayImplementation()) {
      // Overlays do not need any other JsDoc.
      sourceBuilder.appendln("/** @nodts */");
      return;
    }
    StringBuilder sb = new StringBuilder();
    if (type.isInterface()) {
      appendWithNewLine(sb, " * @interface");
    } else if (type.isAbstract()
        || TypeDescriptors.isBoxedTypeAsJsPrimitives(type.getTypeDescriptor())) {
      appendWithNewLine(sb, " * @abstract");
    }
    if (type.getDeclaration().isFinal()) {
      appendWithNewLine(sb, " * @final");
    }
    if (type.getDeclaration().hasTypeParameters()) {
      appendWithNewLine(
          sb,
          " *"
              + getJsDocDeclarationForTypeVariable(
                  type.getDeclaration().getTypeParameterDescriptors()));
    }
    DeclaredTypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor != null && superTypeDescriptor.hasTypeArguments()) {
      // No need to render if it does not have type arguments as it will also appear in the
      // extends clause of the class definition.
      renderClauseIfTypeExistsInJavaScript("extends", superTypeDescriptor, sb);
    }
    String extendsOrImplementsString = type.isInterface() ? "extends" : "implements";
    type.getSuperInterfaceTypeDescriptors()
        .forEach(t -> renderClauseIfTypeExistsInJavaScript(extendsOrImplementsString, t, sb));

    if (type.getDeclaration().isDeprecated()) {
      appendWithNewLine(sb, " * @deprecated");
    }

    String classJsDoc = sb.toString();
    if (!classJsDoc.isEmpty()) {
      sourceBuilder.appendln("/**");
      sourceBuilder.append(classJsDoc);
      sourceBuilder.appendln(" */");
    }
  }

  /*** Renders a JsDoc clause only if the type is an actual class in JavaScript. */
  private void renderClauseIfTypeExistsInJavaScript(
      String extendsOrImplementsString, DeclaredTypeDescriptor typeDescriptor, StringBuilder sb) {
    if (!typeDescriptor.isJavaScriptClass()) {
      return;
    }

    // Don't render the supertype name using the ClosureTypesGenerator to avoid replacement of types
    // like {@code Number} by {@code (Number|number)} in supertype declarations.
    String superTypeString = environment.aliasForType(typeDescriptor.getTypeDeclaration());

    if (typeDescriptor.hasTypeArguments()) {
      superTypeString +=
          typeDescriptor.getTypeArgumentDescriptors().stream()
              // Replace non-native JsEnums with the boxed counterpart since the type
              // arguments on classes that appear in @implements and @extends clauses are
              // rendered explicitly.
              .map(t -> AstUtils.isNonNativeJsEnum(t) ? TypeDescriptors.getEnumBoxType(t) : t)
              .map(closureTypesGenerator::getClosureTypeString)
              .collect(joining(", ", "<", ">"));
    }
    appendWithNewLine(sb, format(" * @%s {%s}", extendsOrImplementsString, superTypeString));
  }

  private void renderClassBody() {
    sourceBuilder.append("class ");
    sourceBuilder.emitWithMapping(
        type.getSourcePosition(),
        () -> sourceBuilder.append(environment.aliasForType(type.getDeclaration())));

    DeclaredTypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor != null && superTypeDescriptor.isJavaScriptClass()) {
      sourceBuilder.append(format(" extends %s", environment.aliasForType(superTypeDescriptor)));
    }

    sourceBuilder.append(" ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    renderTypeMethods();
    renderLoadModules();
    sourceBuilder.closeBrace();
  }

  private void renderTypeMethods() {
    for (Method method : type.getMethods()) {
      if (method.isNative()) {
        // If the method is native, output JsDoc comments so it can serve as a template for
        // native.js. However if the method is pointing to a different namespace then there
        // is no point on doing that since it cannot be provided via a native.js file.
        if (method.getDescriptor().hasJsNamespace()) {
          continue;
        }

        sourceBuilder.emitWithMemberMapping(
            method.getDescriptor(),
            () -> {
              sourceBuilder.append("// ");
              renderMethodJsDoc(method);
              sourceBuilder.append("// native ");
              emitMethodHeader(method);
            });
      } else {

        sourceBuilder.emitWithMemberMapping(
            method.getDescriptor(),
            () -> {
              renderMethodJsDoc(method);
              emitMethodHeader(method);
              StatementTranspiler.render(method.getBody(), environment, sourceBuilder);
            });
      }
      sourceBuilder.newLine();
    }
  }

  private void renderMethodJsDoc(Method method) {
    if (!Strings.isNullOrEmpty(method.getJsDocDescription())) {
      sourceBuilder.appendln("//" + method.getJsDocDescription());
    }
    String jsDoc = getJsDoc(method);
    sourceBuilder.appendln(jsDoc.isEmpty() ? "" : "/**" + jsDoc + " */");
  }

  private String getJsDoc(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    boolean isKotlinSource =
        methodDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration().getSourceLanguage()
            == SourceLanguage.KOTLIN;

    StringBuilder jsDocBuilder = new StringBuilder();

    if (methodDescriptor.getJsVisibility() != Visibility.PUBLIC) {
      jsDocBuilder.append(" @").append(methodDescriptor.getJsVisibility().jsText);
    }

    if (methodDescriptor.isFinal()
        // Don't emit @final on static methods since this are always dispatched statically via
        // collapse properties and j2cl allows the name to be reused. This situation might arise
        // from the use of JsMethod or from Kotlin sources.
        // TODO(b/280321528): remove the special handling for kotlin once this is fixed.
        && !(methodDescriptor.isStatic() && (methodDescriptor.isJsMember() || isKotlinSource))
        // TODO(b/280160727): Remove this when the bug in jscompiler is fixed.
        && !methodDescriptor.isPropertyGetter()
        && !methodDescriptor.isPropertySetter()) {
      jsDocBuilder.append(" @final");
    }
    if (methodDescriptor.isAbstract()) {
      jsDocBuilder.append(" @abstract");
    }
    if (method.getDescriptor().isJsOverride()) {
      jsDocBuilder.append(" @override");
    }
    if (!methodDescriptor.canBeReferencedExternally()
        // TODO(b/193252533): Remove special casing of markImplementor.
        && !methodDescriptor.equals(
            methodDescriptor.getEnclosingTypeDescriptor().getMarkImplementorMethodDescriptor())) {
      jsDocBuilder.append(" @nodts");
    }
    if (methodDescriptor.isSideEffectFree()) {
      jsDocBuilder.append(" @nosideeffects");
    }

    // TODO(b/280315375): Remove the kotlin special case due to disagreement between how we the
    //  classes in the type model vs their implementation.
    if (methodDescriptor.isBridge()
        && (isKotlinSource
            || methodDescriptor.getJsOverriddenMethodDescriptors().stream()
                .anyMatch(m -> m.isFinal()))) {
      // Allow bridges to override final methods.
      jsDocBuilder.append(" @suppress{visibility}");
    }

    if (!methodDescriptor.getTypeParameterTypeDescriptors().isEmpty()) {
      jsDocBuilder.append(
          getJsDocDeclarationForTypeVariable(methodDescriptor.getTypeParameterTypeDescriptors()));
    }

    String returnTypeName =
        closureTypesGenerator.getClosureTypeString(methodDescriptor.getReturnTypeDescriptor());
    if (needsReturnJsDoc(methodDescriptor)) {
      jsDocBuilder.append(" @return {").append(returnTypeName).append("}");
    }

    if (methodDescriptor.isDeprecated()) {
      jsDocBuilder.append(" @deprecated");
    }

    return jsDocBuilder.toString();
  }

  private boolean needsReturnJsDoc(MethodDescriptor methodDescriptor) {
    return !methodDescriptor.isConstructor()
        && !TypeDescriptors.isPrimitiveVoid(methodDescriptor.getReturnTypeDescriptor());
  }

  private void renderLoadModules() {
    MethodDescriptor methodDescriptor = AstUtils.getLoadModulesDescriptor(type.getTypeDescriptor());
    sourceBuilder.newLine();
    sourceBuilder.appendln("/** @nodts */");
    sourceBuilder.append("static " + methodDescriptor.getMangledName() + "() ");
    sourceBuilder.openBrace();

    // goog.module.get(...) for lazy imports.
    imports.stream()
        .filter(i -> i.getImportCategory().needsGoogModuleGet())
        .forEach(
            lazyImport -> {
              String alias = lazyImport.getAlias();
              String path = lazyImport.getImplModulePath();
              sourceBuilder.newLine();
              sourceBuilder.append(alias + " = goog.module.get('" + path + "');");
            });

    sourceBuilder.closeBrace();
  }

  private void renderLoadTimeStatements() {
    type.getLoadTimeStatements()
        .forEach(
            s -> {
              sourceBuilder.newLine();
              StatementTranspiler.render(s, environment, sourceBuilder);
            });
    sourceBuilder.newLine();
  }

  private void renderNativeSource() {
    if (nativeSource == null) {
      return;
    }

    String className = environment.aliasForType(type.getDeclaration());
    sourceBuilder.newLine();
    sourceBuilder.appendln("/* NATIVE.JS EPILOG */");
    sourceBuilder.newLine();

    // A predictable unique alias to current type so native.js could reference it safely.
    String longAliasName =
        type.getDeclaration().getQualifiedSourceName().replace("_", "__").replace('.', '_');
    if (!className.equals(longAliasName)) {
      sourceBuilder.appendln("const " + longAliasName + " = " + className + ";");
      sourceBuilder.newLine();
    }

    int nativeSourceLine = 0;
    int currentByteOffset = 0;
    String content = nativeSource.getContent();
    for (String line : Splitter.on('\n').split(content)) {
      String trimmedLine = CharMatcher.whitespace().trimTrailingFrom(line);

      if (!trimmedLine.isEmpty()) {
        int firstNonWhitespaceColumn = CharMatcher.whitespace().negate().indexIn(trimmedLine);
        sourceBuilder.append(" ".repeat(firstNonWhitespaceColumn));
        // Only map the trimmed section of the line.
        sourceBuilder.emitWithMapping(
            SourcePosition.newBuilder()
                .setStartFilePosition(
                    FilePosition.newBuilder()
                        .setLine(nativeSourceLine)
                        .setColumn(firstNonWhitespaceColumn)
                        .setByteOffset(currentByteOffset + firstNonWhitespaceColumn)
                        .build())
                .setEndFilePosition(
                    FilePosition.newBuilder()
                        .setLine(nativeSourceLine)
                        .setColumn(trimmedLine.length())
                        .setByteOffset(currentByteOffset + trimmedLine.length())
                        .build())
                .setFilePath(nativeSource.getRelativeFilePath())
                .setName(type.getDeclaration().getQualifiedBinaryName() + ".<native>")
                .build(),
            () -> sourceBuilder.append(trimmedLine.substring(firstNonWhitespaceColumn)));
      }
      sourceBuilder.newLine();
      nativeSourceLine++;
      currentByteOffset += line.length() + 1;
    }
  }

  private void renderExports() {
    sourceBuilder.newLine();
    sourceBuilder.appendln("exports = " + environment.aliasForType(type.getDeclaration()) + ";");
  }

  /** Returns the JsDoc declaration clause for a collection of type variables. */
  private String getJsDocDeclarationForTypeVariable(Collection<TypeVariable> typeDescriptors) {
    return format(
        " @template %s",
        typeDescriptors.stream()
            .map(closureTypesGenerator::getClosureTypeString)
            .collect(joining(", ")));
  }

  private static void appendWithNewLine(StringBuilder sb, String string) {
    sb.append(string);
    sb.append("\n");
  }
}
