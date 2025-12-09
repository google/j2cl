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
import static com.google.j2cl.transpiler.backend.closure.ClosureGenerationEnvironment.isDeprecated;
import static java.lang.String.format;

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
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
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
    if (methodDescriptor.isSuspendFunction()) {
      // Kotlin suspend functions are transpiled to JavaScript generator functions, which are
      // indicated by a `*` before their name.
      sourceBuilder.append("*");
    }
    sourceBuilder.emitWithMapping(
        method.getSourcePosition(), () -> sourceBuilder.append(methodDescriptor.getMangledName()));

    environment.emitParameters(sourceBuilder, method);
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
    if (isDeprecated(type.getDeclaration())) {
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
            if (isDeprecated(field.getDescriptor())) {
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
    String classJsDoc = environment.getJsDocForType(type);
    if (classJsDoc.isEmpty()) {
      return;
    }
    if (!classJsDoc.contains("\n")) {
      sourceBuilder.appendln("/**" + classJsDoc + " */");
      return;
    }
    sourceBuilder.appendln("/**");
    sourceBuilder.append(classJsDoc);
    sourceBuilder.appendln(" */");
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
    String jsDoc = environment.getJsDocForMethod(method);
    sourceBuilder.appendln(jsDoc.isEmpty() ? "" : "/**" + jsDoc + " */");
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
}
