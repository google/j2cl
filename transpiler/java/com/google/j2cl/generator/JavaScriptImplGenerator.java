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
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.InitializerBlock;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.common.Problems;
import com.google.j2cl.generator.ImportGatherer.ImportCategory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Generates JavaScript source impl files. */
public class JavaScriptImplGenerator extends JavaScriptGenerator {
  private String nativeSource;
  private String relativeSourceMapLocation;

  private String className;
  private String mangledTypeName;

  protected final StatementTranspiler statementTranspiler;

  public static final String FILE_SUFFIX = ".impl.java.js";

  public JavaScriptImplGenerator(Problems problems, boolean declareLegacyNamespace, Type type) {
    super(problems, declareLegacyNamespace, type);
    this.className = environment.aliasForType(type.getDeclaration());
    this.mangledTypeName =
        ManglingNameUtils.getMangledName(type.getDeclaration().getUnsafeTypeDescriptor());
    this.statementTranspiler = new StatementTranspiler(sourceBuilder, environment);
  }

  private static String getMethodQualifiers(MethodDescriptor methodDescriptor) {
    return methodDescriptor.isStatic() ? "static " : "";
  }

  /** Emits the method header including (static) (getter/setter) methodName(parametersList). */
  private void emitMethodHeader(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    sourceBuilder.append(
        getMethodQualifiers(methodDescriptor) + ManglingNameUtils.getMangledName(methodDescriptor));
    sourceBuilder.append("(");
    String separator = "";
    Variable varargsParameter = method.getJsVarargsParameter();
    for (Variable parameter : method.getParameters()) {
      sourceBuilder.append(separator);
      if (parameter == varargsParameter) {
        sourceBuilder.append("...");
      }
      sourceBuilder.emitWithMapping(
          // Only map parameters if they are named.
          AstUtils.emptySourcePositionIfNotNamed(parameter.getSourcePosition()),
          () -> sourceBuilder.append(environment.aliasForVariable(parameter)));
      separator = ", ";
    }
    sourceBuilder.append(") ");
  }

  @Override
  public String getSuffix() {
    return FILE_SUFFIX;
  }

  public void setRelativeSourceMapLocation(String relativeSourceMapLocation) {
    this.relativeSourceMapLocation = checkNotNull(relativeSourceMapLocation);
  }

  public void setNativeSource(String nativeSource) {
    this.nativeSource = checkNotNull(nativeSource);
  }

  public String getJsDocName(TypeDescriptor typeDescriptor) {
    return JsDocNameUtils.getJsDocName(typeDescriptor, environment);
  }

  public String getJsDocName(TypeDescriptor typeDescriptor, boolean shouldUseClassName) {
    return JsDocNameUtils.getJsDocName(typeDescriptor, shouldUseClassName, environment);
  }

  public String getJsDocNames(Iterable<TypeDescriptor> typeDescriptors) {
    return JsDocNameUtils.getJsDocNames(typeDescriptors, environment);
  }

  @Override
  public String renderOutput() {
    try {
      renderFileOverview(
          "const",
          "extraRequire",
          "missingOverride",
          "missingRequire",
          "suspiciousCode",
          "transitionalSuspiciousCodeWarnings",
          "unusedLocalVariables",
          "uselessCode");
      renderImports();
      renderTypeAnnotation();
      renderTypeBody();
      renderStaticFieldDeclarations();
      renderMarkImplementorCalls();
      renderNativeSource();
      renderExports();
      renderSourceMapLocation();
      return sourceBuilder.build();
    } catch (RuntimeException e) {
      // Catch all unchecked exceptions and rethrow them with more context to make debugging easier.
      // Yes this is really being done on purpose.
      throw new RuntimeException(
          "Error generating source for type " + type.getDeclaration().getQualifiedBinaryName(), e);
    }
  }

  private void renderImports() {
    TypeDeclaration typeDeclaration = type.getDeclaration();

    // goog.module(...) declaration.
    sourceBuilder.appendln("goog.module('" + typeDeclaration.getImplModuleName() + "');");
    if (declareLegacyNamespace && AstUtils.canBeRequiredFromJs(typeDeclaration)) {
      // Even if opted into declareLegacyNamespace, this only makes sense for classes that are
      // intended to be accessed from the native JS. Thus we only emit declareLegacyNamespace
      // for non-anonymous JsType classes.
      sourceBuilder.appendln("goog.module.declareLegacyNamespace();");
    }
    sourceBuilder.newLines(2);

    // goog.require(...) for eager imports.
    Map<String, String> aliasesByPath = new HashMap<>();
    Iterable<Import> eagerImports = sortImports(importsByCategory.get(ImportCategory.EAGER));
    for (Import eagerImport : eagerImports) {
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
    }
    if (!Iterables.isEmpty(eagerImports)) {
      sourceBuilder.newLine();
    }

    // goog.forwardDeclare(...) for lazy imports.
    Iterable<Import> lazyImports = sortImports(importsByCategory.get(ImportCategory.LAZY));
    for (Import lazyImport : lazyImports) {
      String alias = lazyImport.getAlias();
      String path = lazyImport.getImplModulePath();
      sourceBuilder.appendln("let " + alias + " = goog.forwardDeclare('" + path + "');");
    }
    if (!Iterables.isEmpty(lazyImports)) {
      sourceBuilder.newLine();
    }

    sourceBuilder.newLine();
  }

  private void renderTypeAnnotation() {
    if (type.isJsOverlayImplementation()) {
      // Do nothing.
    } else if (type.isInterface()) {
      sourceBuilder.appendLines("/**", " * @interface");
      sourceBuilder.newLine();
      if (type.getDeclaration().hasTypeParameters()) {
        String templates = getJsDocNames(type.getDeclaration().getTypeParameterDescriptors());
        sourceBuilder.appendln(" * @template " + templates);
      }
      for (TypeDescriptor superInterfaceType : type.getSuperInterfaceTypeDescriptors()) {
        if (!superInterfaceType.isStarOrUnknown()) {
          sourceBuilder.appendln(" * @extends {" + getJsDocName(superInterfaceType, true) + "}");
        }
      }
      sourceBuilder.appendln(" */");
    } else { // Not an interface so it is a Class.
      SourceBuilder buffer = new SourceBuilder();
      if (type.isAbstract()) {
        buffer.appendln(" * @abstract");
      }
      if (type.getDeclaration().hasTypeParameters()) {
        String templates = getJsDocNames(type.getDeclaration().getTypeParameterDescriptors());
        buffer.appendln(" * @template " + templates);
      }
      if (type.getSuperTypeDescriptor() != null
          && type.getSuperTypeDescriptor().hasTypeArguments()
          && !type.getSuperTypeDescriptor().isStarOrUnknown()) {
        String supertype = getJsDocName(type.getSuperTypeDescriptor(), true);
        buffer.appendln(" * @extends {" + supertype + "}");
      }
      for (TypeDescriptor superInterfaceType : type.getSuperInterfaceTypeDescriptors()) {
        if (!superInterfaceType.isStarOrUnknown()) {
          buffer.appendln(" * @implements {" + getJsDocName(superInterfaceType, true) + "}");
        }
      }

      String annotation = buffer.build();
      if (!annotation.isEmpty()) {
        sourceBuilder.appendln("/**");
        sourceBuilder.append(annotation);
        sourceBuilder.appendln(" */");
      }
    }
  }

  private void renderTypeBody() {
    String extendsClause = GeneratorUtils.getExtendsClause(type, environment);
    sourceBuilder.append("class " + className + " " + extendsClause);
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    environment.setEnclosingTypeDescriptor(type.getDeclaration().getUnsafeTypeDescriptor());
    renderTypeMethods();
    renderMarkImplementorMethod();
    renderIsInstanceMethod();
    renderIsAssignableFromMethod();
    renderCopyMethod();
    renderClinit();
    renderInitMethod();
    environment.setEnclosingTypeDescriptor(null);
    sourceBuilder.closeBrace();
    sourceBuilder.append(";");
    sourceBuilder.newLines(2);
    renderClassMetadata();
    sourceBuilder.newLines(2);
  }

  private void renderTypeMethods() {
    for (Method method : type.getMethods()) {
      // JsOverlay methods are rendered in their own files.
      if (method.getDescriptor().isJsOverlay()) {
        continue;
      }

      if (method.isNative()) {
        // If the method is native, output JsDoc comments so it can serve as a template for
        // native.js. However if the method is pointing to a different namespace then there
        // is no point on doing that since it cannot be provided via a native.js file.
        if (method.getDescriptor().hasJsNamespace()) {
          continue;
        }
        renderMethodJsDoc(method);
        sourceBuilder.append("// native ");
        emitMethodHeader(method);
      } else {
        renderMethodJsDoc(method);
        emitMethodHeader(method);
        statementTranspiler.renderStatement(method.getBody());
      }
      sourceBuilder.newLines(2);
    }
  }

  private void renderMethodJsDoc(Method method) {
    sourceBuilder.appendln("/**");
    if (method.getJsDocDescription() != null && !method.getJsDocDescription().isEmpty()) {
      sourceBuilder.appendln(" * " + method.getJsDocDescription());
    }
    if (method.isAbstract()) {
      sourceBuilder.appendln(" * @abstract");
    }
    if (method.isOverride() && !method.isConstructor()) {
      sourceBuilder.appendln(" * @override");
    }
    if (!method.getDescriptor().getTypeParameterTypeDescriptors().isEmpty()) {
      String templateParamNames =
          JsDocNameUtils.getJsDocNames(
              method.getDescriptor().getTypeParameterTypeDescriptors(), environment);
      sourceBuilder.appendln(" * @template " + templateParamNames);
    }

    if (type.getDeclaration().isJsFunctionImplementation()
        && method.getDescriptor().isPolymorphic()
        && !method.getBody().getStatements().isEmpty()
        && !method.getDescriptor().getName().startsWith("$ctor")) {
      sourceBuilder.appendln(
          " * @this {" + getJsDocName(type.getDeclaration().getUnsafeTypeDescriptor()) + "}");
    }
    for (int i = 0; i < method.getParameters().size(); i++) {
      sourceBuilder.appendln(
          " * " + GeneratorUtils.getParameterJsDocAnnotation(method, i, environment));
    }
    String returnTypeName = getJsDocName(method.getDescriptor().getReturnTypeDescriptor());
    if (!method.getDescriptor().isConstructor()) {
      sourceBuilder.appendln(" * @return {" + returnTypeName + "}");
    }
    sourceBuilder.appendln(" * @" + GeneratorUtils.visibilityForMethod(method));
    sourceBuilder.appendln(" */");
  }

  private void renderMarkImplementorMethod() {
    if (!type.isInterface() || type.isJsOverlayImplementation()) {
      return; // Only render markImplementor code for interfaces.
    }
    sourceBuilder.appendLines(
        "/**",
        " * Marks the provided class as implementing this interface.",
        " * @param {"
            + TypeDescriptors.get().nativeFunction.getQualifiedJsName()
            + "} classConstructor",
        " * @public",
        " */",
        "static $markImplementor(classConstructor) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    for (TypeDescriptor superInterface : type.getSuperInterfaceTypeDescriptors()) {
      if (superInterface.isNative()) {
        continue;
      }
      String superInterfaceName = environment.aliasForType(superInterface);
      sourceBuilder.appendln(superInterfaceName + ".$markImplementor(classConstructor);");
    }
    sourceBuilder.appendLines(
        "/**",
        " * @public {boolean}",
        " */",
        "classConstructor.prototype.$implements__" + mangledTypeName + " = true;");
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO(tdeegan): Move this to the ast in a normalization pass.
  private void renderIsInstanceMethod() {
    if (type.containsMethod(MethodDescriptor.IS_INSTANCE_METHOD_NAME)) {
      sourceBuilder.appendLines(
          "/**", " * $isInstance() function implementation is provided separately.", " */");
      sourceBuilder.newLine();
      return;
    }
    if (type.isInterface()) {
      renderIsInstanceForInterfaceType();
    } else {
      checkState(type.isClass() || type.isEnum());
      renderIsInstanceForClassType();
    }
  }

  private void renderIsInstanceForClassType() {
    sourceBuilder.appendLines(
        "/**",
        " * Returns whether the provided instance is an instance of this class.",
        " * @param {*} instance",
        " * @return {boolean}",
        " * @public",
        " */",
        "static $isInstance(instance) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    if (type.getDeclaration().isJsFunctionImplementation()) {
      sourceBuilder.appendln("return instance != null && !!instance.$is__" + mangledTypeName + ";");
    } else {
      String className =
          environment.aliasForType(
              type.isJsOverlayImplementation()
                  ? type.getNativeTypeDescriptor().getRawTypeDescriptor()
                  : type.getDeclaration().getUnsafeTypeDescriptor());
      sourceBuilder.append("return instance instanceof " + className + ";");
    }
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  private void renderIsInstanceForInterfaceType() {
    sourceBuilder.appendLines(
        "/**",
        " * Returns whether the provided instance is of a class that implements this",
        " * interface.",
        " * @param {*} instance",
        " * @return {boolean}",
        " * @public",
        " */",
        "static $isInstance(instance) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();
    if (type.isJsOverlayImplementation()) {
      sourceBuilder.append("return true;");
    } else if (type.getDeclaration().isJsFunctionInterface()) {
      sourceBuilder.append("return instance != null && typeof instance == \"function\";");
    } else {
      sourceBuilder.append(
          "return instance != null && !!instance.$implements__" + mangledTypeName + ";");
    }
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO(tdeegan): Move this to the ast in a normalization pass.
  private void renderIsAssignableFromMethod() {
    if (type.isJsOverlayImplementation()
        || type.containsMethod(MethodDescriptor.IS_ASSIGNABLE_FROM_METHOD_NAME)) {
      return; // Don't render for overlay types or if the method exists.
    }
    sourceBuilder.appendLines(
        "/**",
        " * Returns whether the provided class is or extends this class.",
        " * @param {"
            + TypeDescriptors.get().nativeFunction.getQualifiedJsName()
            + "} classConstructor",
        " * @return {boolean}",
        " * @public",
        " */",
        "static $isAssignableFrom(classConstructor) ");
    sourceBuilder.openBrace();
    sourceBuilder.newLine();

    if (type.isInterface()) { // For interfaces
      sourceBuilder.append(
          "return classConstructor != null && !!classConstructor.prototype.$implements__"
              + mangledTypeName
              + ";");
    } else { // For classes
      BootstrapType.NATIVE_UTIL.getDescriptor();
      String utilAlias = environment.aliasForType(BootstrapType.NATIVE_UTIL.getDescriptor());
      sourceBuilder.append(
          "return " + utilAlias + ".$canCastClass(classConstructor, " + className + ");");
    }
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO(tdeegan): Move this to the ast in a normalization pass.
  // TODO(tdeegan): may copy Objects methods (equals, hashCode, etc. ) as well.
  private void renderCopyMethod() {
    if (!type.getDeclaration().isJsFunctionImplementation()) {
      return; // Only render the $copy method for jsfunctions
    }
    sourceBuilder.appendLines(
        "/**",
        " * Copies the fields from {@code from} to {@code to}.",
        " * @param {" + className + "} from",
        " * @param {*} to",
        " * @public",
        " */",
        "static $copy(from, to) ");
    sourceBuilder.openBrace();
    for (Field field : type.getInstanceFields()) {
      String fieldName = ManglingNameUtils.getMangledName(field.getDescriptor());
      sourceBuilder.newLine();
      sourceBuilder.append("to." + fieldName + " = from." + fieldName + ";");
    }
    sourceBuilder.newLine();
    sourceBuilder.appendLines(
        "// Marks the object is an instance of this class.",
        "to.$is__" + mangledTypeName + " = true;");
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO(tdeegan): Move this to the ast in a normalization pass.
  private void renderClassMetadata() {
    String utilAlias = environment.aliasForType(BootstrapType.NATIVE_UTIL.getDescriptor());

    String name = null;
    if (type.isJsOverlayImplementation()) {
      name = type.getNativeTypeDescriptor().getQualifiedJsName();
    } else if (type.getDeclaration().isJsFunctionInterface()) {
      name = "Function";
    } else {
      name = type.getDeclaration().getQualifiedBinaryName();
    }

    String obfuscatableName = utilAlias + ".$makeClassName('" + name + "')";
    if (type.isInterface()) {
      sourceBuilder.appendln(
          utilAlias
              + ".$setClassMetadataForInterface("
              + className
              + ", "
              + obfuscatableName
              + ");");
    } else if (type.isEnum()) {
      sourceBuilder.appendln(
          utilAlias + ".$setClassMetadataForEnum(" + className + ", " + obfuscatableName + ");");
    } else {
      sourceBuilder.appendln(
          utilAlias + ".$setClassMetadata(" + className + ", " + obfuscatableName + ");");
    }
  }

  // TODO(tdeegan): Move this to the ast in a normalization pass.
  private void renderClinit() {
    renderInitializerMethodHeader(
        AstUtils.getClinitMethodDescriptor(type.getDeclaration().getUnsafeTypeDescriptor()),
        "Runs inline static field initializers.");
    sourceBuilder.openBrace();

    // Set this method to reference an empty function so that it will not be executed again.
    sourceBuilder.newLine();
    sourceBuilder.append(className + ".$clinit = function() {};");

    // goog.module.get(...) for lazy imports.
    for (Import lazyImport : sortImports(importsByCategory.get(ImportCategory.LAZY))) {
      String alias = lazyImport.getAlias();
      String path = lazyImport.getImplModulePath();
      sourceBuilder.newLine();
      sourceBuilder.append(alias + " = goog.module.get('" + path + "');");
    }

    // Static field and static initializer blocks.
    renderInitializerElements(type.getStaticInitializerBlocks());

    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  // TODO(b/34928687): Move this to the ast in a normalization pass.
  private void renderInitMethod() {
    if (type.isJsOverlayImplementation() || type.isInterface()) {
      return;
    }
    renderInitializerMethodHeader(
        AstUtils.getInitMethodDescriptor(type.getDeclaration().getUnsafeTypeDescriptor()),
        "Runs instance field and block initializers.");
    sourceBuilder.openBrace();
    renderInitializerElements(type.getInstanceInitializerBlocks());
    sourceBuilder.closeBrace();
    sourceBuilder.newLines(2);
  }

  private void renderInitializerMethodHeader(
      MethodDescriptor methodDescriptor, String description) {
    sourceBuilder.appendLines(
        "/**",
        " * " + description,
        " * " + (methodDescriptor.getVisibility().isPrivate() ? "@private" : "@public"),
        " */",
        (methodDescriptor.isStatic() ? "static " : "")
            + ManglingNameUtils.getMangledName(methodDescriptor)
            + "() ");
  }

  private void renderInitializerElements(Collection<InitializerBlock> initializerBlocks) {
    for (InitializerBlock initializerBlock : initializerBlocks) {
      for (Statement initializer : initializerBlock.getBlock().getStatements()) {
        sourceBuilder.newLine();
        statementTranspiler.renderStatement(initializer);
      }
    }
  }

  private void renderStaticFieldDeclarations() {
    for (Field staticField : type.getStaticFields()) {
      String jsDocType =
          JsDocNameUtils.getJsDocName(staticField.getDescriptor().getTypeDescriptor(), environment);

      if (staticField.isCompileTimeConstant()) {
        sourceBuilder.appendLines("/**", " * @public {" + jsDocType + "}", " * @const", " */");
      } else {
        sourceBuilder.appendLines("/**", " * @private {" + jsDocType + "}", " */");
      }

      sourceBuilder.newLine();
      String fieldName = ManglingNameUtils.getMangledName(staticField.getDescriptor());
      sourceBuilder.append(className + "." + fieldName + " = ");
      renderExpression(getInitialValue(staticField));
      sourceBuilder.append(";");
      // emit 2 empty lines
      sourceBuilder.newLines(3);
    }
  }

  private static Expression getInitialValue(Field field) {
    return field.getInitializer() != null
        ? field.getInitializer()
        : TypeDescriptors.getDefaultValue(field.getDescriptor().getTypeDescriptor());
  }

  /**
   * Here we call markImplementor on all interfaces such that the class can be queried at run time
   * to determine if it implements an interface.
   */
  private void renderMarkImplementorCalls() {
    if (type.isJsOverlayImplementation()) {
      return; // Do nothing
    }
    if (type.isInterface()) {
      // TODO(b/20102666): remove cast after b/20102666 is handled in Closure.
      sourceBuilder.appendln(
          className
              + ".$markImplementor(/** @type {"
              + TypeDescriptors.get().nativeFunction.getQualifiedJsName()
              + "} */ ("
              + className
              + "));");
    } else { // Not an interface so it is a Class.
      for (TypeDescriptor interfaceTypeDescriptor : type.getSuperInterfaceTypeDescriptors()) {
        if (interfaceTypeDescriptor.isNative()) {
          continue;
        }
        String interfaceName = environment.aliasForType(interfaceTypeDescriptor);
        sourceBuilder.appendln(interfaceName + ".$markImplementor(" + className + ");");
      }
    }
    sourceBuilder.newLines(2);
  }

  private void renderNativeSource() {
    if (nativeSource != null) {
      sourceBuilder.appendLines("/**", " * Native Method Injection", " */");
      String longAliasName = type.getDeclaration().getUnsafeTypeDescriptor().getLongAliasName();
      if (!className.equals(longAliasName)) {
        sourceBuilder.appendLines(
            "// Long alias for the class defined in this module",
            "const " + longAliasName + " = " + className + ";");
      }
      sourceBuilder.newLine();
      sourceBuilder.appendln(nativeSource);
      sourceBuilder.newLine();
    }
  }

  private void renderExports() {
    sourceBuilder.appendLines("/**", " * Export class.", " */", "exports = " + className + ";");
    sourceBuilder.newLine();
  }

  private void renderSourceMapLocation() {
    if (relativeSourceMapLocation != null) {
      sourceBuilder.append("//# sourceMappingURL=" + relativeSourceMapLocation);
    }
  }

  private void renderExpression(Expression expression) {
    ExpressionTranspiler.render(expression, environment, sourceBuilder);
  }
}
