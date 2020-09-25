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
package com.google.j2cl.generator;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodLike;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.TypeVariable;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.common.FilePosition;
import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourcePosition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Generates JavaScript source impl files. */
public class JavaScriptImplGenerator extends JavaScriptGenerator {
  private NativeJavaScriptFile nativeSource;
  private final ClosureTypesGenerator closureTypesGenerator;

  protected final StatementTranspiler statementTranspiler;

  public static final String FILE_SUFFIX = ".impl.java.js";

  public JavaScriptImplGenerator(Problems problems, Type type, List<Import> imports) {
    super(problems, type, imports);
    this.statementTranspiler = new StatementTranspiler(sourceBuilder, environment);
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
                field.getSourcePosition(),
                () -> {
                  sourceBuilder.append(field.getDescriptor().getMangledName());
                });
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
            .collect(ImmutableList.toImmutableList()),
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
            .collect(ImmutableList.toImmutableList()),
        lazyImport -> {
          String alias = lazyImport.getAlias();
          String path = lazyImport.getImplModulePath();
          sourceBuilder.appendln("let " + alias + " = goog.forwardDeclare('" + path + "');");
        });
  }

  private void renderTypeAnnotation() {
    if (type.isOverlayImplementation()) {
      // Do nothing.
    } else if (type.isInterface()) {
      sourceBuilder.appendLines("/**", " * @interface");
      sourceBuilder.newLine();
      if (type.getDeclaration().hasTypeParameters()) {
        String templates =
            closureTypesGenerator.getCommaSeparatedClosureTypesString(
                type.getDeclaration().getTypeParameterDescriptors());
        sourceBuilder.appendln(" * @template " + templates);
      }
      for (DeclaredTypeDescriptor superInterfaceType : type.getSuperInterfaceTypeDescriptors()) {
        renderIfClassExists(" * @extends {%s}", superInterfaceType, sourceBuilder);
      }
      if (type.getDeclaration().isDeprecated()) {
        sourceBuilder.appendln(" * @deprecated");
      }
      sourceBuilder.appendln(" */");
    } else { // Not an interface so it is a Class.
      SourceBuilder buffer = new SourceBuilder();
      if (type.isAbstract()) {
        buffer.appendln(" * @abstract");
      }
      if (type.getDeclaration().hasTypeParameters()) {
        String templates =
            closureTypesGenerator.getCommaSeparatedClosureTypesString(
                type.getDeclaration().getTypeParameterDescriptors());
        buffer.appendln(" * @template " + templates);
      }
      if (type.getDeclaration().isDeprecated()) {
        buffer.appendln(" * @deprecated");
      }
      if (type.getSuperTypeDescriptor() != null
          && type.getSuperTypeDescriptor().hasTypeArguments()) {
        // No need to render if it does not have type arguments as it will also appear in the
        // extends clause of the class definition.
        renderIfClassExists(" * @extends {%s}", type.getSuperTypeDescriptor(), buffer);
      }
      for (DeclaredTypeDescriptor superInterfaceType : type.getSuperInterfaceTypeDescriptors()) {
        renderIfClassExists(" * @implements {%s}", superInterfaceType, buffer);
      }

      String annotation = buffer.build();
      if (!annotation.isEmpty()) {
        sourceBuilder.appendln("/**");
        sourceBuilder.append(annotation);
        sourceBuilder.appendln(" */");
      }
    }
  }

  /**
   * Renders the line using {@code formatString} only if {@code typeDescriptor} is an actual class
   * in JavaScript.
   *
   * <p>Used to render the @extends/@implements clauses.
   */
  private void renderIfClassExists(
      String formatString, DeclaredTypeDescriptor typeDescriptor, SourceBuilder sourceBuilder) {
    if (doesClassExistInJavaScript(typeDescriptor)) {
      String typeArgumentsString =
          typeDescriptor.hasTypeArguments()
              ? typeDescriptor.getTypeArgumentDescriptors().stream()
                  .map(td -> closureTypesGenerator.getClosureTypeString(td))
                  .collect(Collectors.joining(", ", "<", ">"))
              : "";

      sourceBuilder.appendln(
          String.format(
              formatString,
              environment.aliasForType(typeDescriptor.getTypeDeclaration()) + typeArgumentsString));
    }
  }

  private boolean doesClassExistInJavaScript(DeclaredTypeDescriptor type) {
    return !type.isStarOrUnknown() && !type.isJsFunctionInterface();
  }

  private void renderClassBody() {
    sourceBuilder.append("class ");
    sourceBuilder.emitWithMapping(
        type.getSourcePosition(),
        () -> sourceBuilder.append(environment.aliasForType(type.getDeclaration())));
    sourceBuilder.append(" " + getExtendsClause(type, environment));
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    renderTypeMethods();
    renderCopyMethod();
    renderLoadModules();
    sourceBuilder.closeBrace();
  }

  private static String getExtendsClause(Type type, GenerationEnvironment environment) {
    DeclaredTypeDescriptor superTypeDescriptor = type.getSuperTypeDescriptor();
    if (superTypeDescriptor == null || superTypeDescriptor.isStarOrUnknown()) {
      return "";
    }
    String superTypeName = environment.aliasForType(superTypeDescriptor);
    return String.format("extends %s ", superTypeName);
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
              statementTranspiler.renderStatement(method.getBody());
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

    StringBuilder jsDocBuilder = new StringBuilder();
    if (methodDescriptor.getJsVisibility() != Visibility.PUBLIC) {
      jsDocBuilder.append(" @" + methodDescriptor.getJsVisibility().jsText);
    }
    if (methodDescriptor.isAbstract()) {
      jsDocBuilder.append(" @abstract");
    }
    if (method.getDescriptor().isJsOverride()) {
      jsDocBuilder.append(" @override");
    }
    if (!methodDescriptor.getTypeParameterTypeDescriptors().isEmpty()) {
      String templateParamNames =
          closureTypesGenerator.getCommaSeparatedClosureTypesString(
              methodDescriptor.getTypeParameterTypeDescriptors());
      jsDocBuilder.append(" @template " + templateParamNames);
    }

    if (type.getDeclaration().isJsFunctionImplementation()
        && methodDescriptor.isInstanceMember()
        && !method.getBody().getStatements().isEmpty()) {
      // TODO(b/120800425): Solve the object<->function duality in JsFunction implementations in a
      // more principled way.
      if (methodDescriptor.getName().startsWith("$ctor")) {
        // ctor treats the JsFunction implementation as a class to invoke java.lang.Object ctor
        // method. Do not redefine @this, instead suppress invalid casts to allow casting this to
        // a function if needed.
        jsDocBuilder.append(" @suppress {invalidCasts}");
      } else {
        // Using @this redefines enclosing class of a method, hence any template variables defined
        // in the class need to be declared in the method.
        for (TypeVariable typeVariable : type.getDeclaration().getTypeParameterDescriptors()) {
          jsDocBuilder.append(
              " @template " + closureTypesGenerator.getClosureTypeString(typeVariable));
        }
        jsDocBuilder.append(
            " @this {"
                + closureTypesGenerator.getClosureTypeString(type.getTypeDescriptor())
                + "}");
      }
    }
    String returnTypeName =
        closureTypesGenerator.getClosureTypeString(methodDescriptor.getReturnTypeDescriptor());
    if (needsReturnJsDoc(methodDescriptor)) {
      jsDocBuilder.append(" @return {" + returnTypeName + "}");
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

  // TODO(b/34928687): Move this to the ast in a normalization pass.
  // TODO(b/80269359): may copy Objects methods (equals, hashCode, etc. ) as well.
  private void renderCopyMethod() {
    if (!type.getDeclaration().isJsFunctionImplementation()) {
      return; // Only render the $copy method for jsfunctions
    }
    sourceBuilder.appendln(
        "static $copy(/**"
            + environment.aliasForType(type.getDeclaration())
            + "*/ from, /** ? */ to) ");
    sourceBuilder.openBrace();
    for (Field field : type.getInstanceFields()) {
      String fieldName = field.getDescriptor().getMangledName();
      sourceBuilder.newLine();
      sourceBuilder.append("to." + fieldName + " = from." + fieldName + ";");
    }
    sourceBuilder.newLine();
    sourceBuilder.appendLines(
        "// Marks the object is an instance of this class.",
        "to.$is__" + type.getDeclaration().getMangledName() + " = true;");
    sourceBuilder.closeBrace();
    sourceBuilder.newLine();
  }

  // TODO(b/67965153): Move this to the ast in a normalization pass.
  private void renderClassMetadata() {
    if (type.isOverlayImplementation()
        && (type.getOverlaidTypeDeclaration().isJsFunctionInterface()
            || type.getOverlaidTypeDeclaration().isInterface())) {
      // JsFunction and Native interface overlays do not need class metadata.
      return;
    }
    sourceBuilder.newLine();

    String utilAlias = environment.aliasForType(BootstrapType.NATIVE_UTIL.getDescriptor());

    TypeDeclaration targetTypeDescriptor = type.getUnderlyingTypeDeclaration();

    String name =
        targetTypeDescriptor.isNative()
            // For native types the qualified JavaScript name is more useful to identify the
            // type, in particular for debugging.
            ? targetTypeDescriptor.getQualifiedJsName()
            : targetTypeDescriptor.getQualifiedBinaryName();

    String obfuscatableName = "'" + name + "'";
    String className = environment.aliasForType(type.getDeclaration());
    if (targetTypeDescriptor.isInterface()) {
      sourceBuilder.append(
          utilAlias
              + ".$setClassMetadataForInterface("
              + className
              + ", "
              + obfuscatableName
              + ");");
    } else if (targetTypeDescriptor.isEnum() && !targetTypeDescriptor.isJsEnum()) {
      // TODO(b/117525773): targetTypeDescriptor.isEnum should already be false for JsEnums,
      // making the second part of the condition unnecessary.
      sourceBuilder.append(
          utilAlias + ".$setClassMetadataForEnum(" + className + ", " + obfuscatableName + ");");
    } else {
      sourceBuilder.append(
          utilAlias + ".$setClassMetadata(" + className + ", " + obfuscatableName + ");");
    }
  }

  private void renderLoadModules() {
    MethodDescriptor methodDescriptor = AstUtils.getLoadModulesDescriptor(type.getTypeDescriptor());
    sourceBuilder.newLine();
    sourceBuilder.appendLines("static " + methodDescriptor.getMangledName() + "() ");
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
    renderClassMetadata();
    statementTranspiler.renderStatements(type.getLoadTimeStatements());
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
        sourceBuilder.append(Strings.repeat(" ", firstNonWhitespaceColumn));
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
    sourceBuilder.appendLines("exports = " + environment.aliasForType(type.getDeclaration()) + ";");
    // TODO(b/77961191): add a new line once the bug is resolved.
  }

}
