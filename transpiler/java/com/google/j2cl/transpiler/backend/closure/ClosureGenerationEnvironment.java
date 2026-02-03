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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.joining;

import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.HasAnnotations;
import com.google.j2cl.transpiler.ast.HasName;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.SourceLanguage;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.Visibility;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Contains aliases for variables and Type Descriptors. */
public class ClosureGenerationEnvironment {
  /**
   * A map from type binary named (e.g. a.b.Foo) to alias for that type. Keyed by binary name so
   * generic and non-generic permutations of a class all map to the same type.
   */
  private final Map<String, String> aliasByTypeBinaryName = new HashMap<>();

  private final Map<HasName, String> uniqueNameByVariable;

  private final ClosureTypesGenerator closureTypesGenerator = new ClosureTypesGenerator(this);

  public ClosureGenerationEnvironment(
      Collection<Import> imports, Map<HasName, String> uniqueNameByVariable) {
    for (Import anImport : imports) {
      String alias = anImport.getAlias();
      checkArgument(alias != null && !alias.isEmpty(), "Bad alias for %s", anImport.getElement());
      aliasByTypeBinaryName.put(anImport.getElement().getQualifiedBinaryName(), alias);
    }
    this.uniqueNameByVariable = uniqueNameByVariable;
  }

  public String getUniqueNameForVariable(HasName variable) {
    if (uniqueNameByVariable.containsKey(variable)) {
      return uniqueNameByVariable.get(variable);
    }
    return variable.getName();
  }

  public String aliasForType(DeclaredTypeDescriptor typeDescriptor) {
    return aliasForType(typeDescriptor.getTypeDeclaration());
  }

  public String aliasForType(TypeDeclaration typeDeclaration) {
    if (typeDeclaration.isExtern()) {
      return typeDeclaration.getQualifiedJsName();
    }
    String moduleAlias =
        aliasByTypeBinaryName.get(typeDeclaration.getEnclosingModule().getQualifiedBinaryName());
    checkState(
        moduleAlias != null, "An alias was needed for %s but no alias was found.", typeDeclaration);

    String innerTypeQualifier = typeDeclaration.getInnerTypeQualifier();

    return innerTypeQualifier.isEmpty() ? moduleAlias : moduleAlias + "." + innerTypeQualifier;
  }

  public String getClosureTypeString(TypeDescriptor typeDescriptor) {
    return closureTypesGenerator.getClosureTypeString(typeDescriptor);
  }

  public String getJsDocForParameter(MethodLike methodLike, int index) {
    return closureTypesGenerator.getJsDocForParameter(methodLike, index);
  }

  public String getJsDocForReturn(MethodLike methodLike) {
    return closureTypesGenerator.getJsDocForReturnType(methodLike.getDescriptor());
  }

  /** Returns the JsDoc annotation for the given type. */
  public String getJsDocForType(Type type) {
    return getJsDocForType(type, /* isWasmExtern= */ false);
  }

  /** Returns the JsDoc annotation for the given type. */
  public String getJsDocForType(Type type, boolean isWasmExtern) {
    if (type.isOverlayImplementation()) {
      // Overlays do not need any other JsDoc.
      return " @nodts";
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
    if (superTypeDescriptor != null && (isWasmExtern || superTypeDescriptor.hasTypeArguments())) {
      // No need to render if it does not have type arguments as it will also appear in the
      // extends clause of the class definition (unless it's an externs declaration).
      renderClauseIfTypeExistsInJavaScript("extends", superTypeDescriptor, isWasmExtern, sb);
    }

    // TODO(b/459918329): Support interfaces in Wasm externs.
    if (!isWasmExtern) {
      String extendsOrImplementsString = type.isInterface() ? "extends" : "implements";
      type.getSuperInterfaceTypeDescriptors()
          .forEach(
              t ->
                  renderClauseIfTypeExistsInJavaScript(
                      extendsOrImplementsString, t, isWasmExtern, sb));
    }

    if (isDeprecated(type.getDeclaration())) {
      appendWithNewLine(sb, " * @deprecated");
    }

    return sb.toString();
  }

  /** Returns the JsDoc declaration clause for a collection of type variables. */
  private String getJsDocDeclarationForTypeVariable(Collection<TypeVariable> typeDescriptors) {
    return String.format(
        " @template %s",
        typeDescriptors.stream()
            .map(closureTypesGenerator::getClosureTypeString)
            .collect(joining(", ")));
  }

  /*** Renders a JsDoc clause only if the type is an actual class in JavaScript. */
  private void renderClauseIfTypeExistsInJavaScript(
      String extendsOrImplementsString,
      DeclaredTypeDescriptor typeDescriptor,
      boolean isWasmExtern,
      StringBuilder sb) {
    if (!typeDescriptor.isJavaScriptClass()) {
      return;
    }

    // Don't render the supertype name using the ClosureTypesGenerator to avoid replacement of types
    // like {@code Number} by {@code (Number|number)} in supertype declarations.
    String superTypeString = aliasForType(typeDescriptor.getTypeDeclaration());

    if (typeDescriptor.hasTypeArguments()) {
      superTypeString +=
          typeDescriptor.getTypeArgumentDescriptors().stream()
              // Replace non-native JsEnums with the boxed counterpart since the type
              // arguments on classes that appear in @implements and @extends clauses are
              // rendered explicitly.
              .map(
                  t ->
                      // TODO(b/479895127): Consider JsEnums for Wasm externs. Currently
                      // just output `t` if we encounter an enum.
                      (!isWasmExtern && AstUtils.isNonNativeJsEnum(t))
                          ? TypeDescriptors.getEnumBoxType(t)
                          : t)
              .map(closureTypesGenerator::getClosureTypeString)
              .collect(joining(", ", "<", ">"));
    }
    appendWithNewLine(sb, String.format(" * @%s {%s}", extendsOrImplementsString, superTypeString));
  }

  /** Returns the JsDoc annotation for the given method. */
  public String getJsDocForMethod(Method method) {
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
                .anyMatch(MemberDescriptor::isFinal))) {
      // Allow bridges to override final methods.
      jsDocBuilder.append(" @suppress{visibility}");
    }

    if (!methodDescriptor.getTypeParameterTypeDescriptors().isEmpty()) {
      jsDocBuilder.append(
          getJsDocDeclarationForTypeVariable(methodDescriptor.getTypeParameterTypeDescriptors()));
    }

    if (needsReturnJsDoc(methodDescriptor)) {
      String returnTypeName = closureTypesGenerator.getJsDocForReturnType(methodDescriptor);
      jsDocBuilder.append(" @return {").append(returnTypeName).append("}");
    }

    if (isDeprecated(methodDescriptor)) {
      jsDocBuilder.append(" @deprecated");
    }

    return jsDocBuilder.toString();
  }

  private boolean needsReturnJsDoc(MethodDescriptor methodDescriptor) {
    return !methodDescriptor.isConstructor()
        && (!TypeDescriptors.isPrimitiveVoid(methodDescriptor.getReturnTypeDescriptor())
            // Suspend functions are transpiled to JS Generator functions which always require
            // to declare the return type even if the generator does not yield any value.
            || methodDescriptor.isSuspendFunction());
  }

  private static void appendWithNewLine(StringBuilder sb, String string) {
    sb.append(string);
    sb.append("\n");
  }

  /** Returns true if the given node is annotated with @Deprecated. */
  public static boolean isDeprecated(HasAnnotations hasAnnotations) {
    return hasAnnotations.hasAnnotation("java.lang.Deprecated")
        || hasAnnotations.hasAnnotation("kotlin.Deprecated");
  }

  /** Emits the comma separated list of parameter annotated with their respective types. */
  public void emitParameters(SourceBuilder sourceBuilder, MethodLike method) {
    sourceBuilder.append("(");
    String separator = "";
    for (int i = 0; i < method.getParameters().size(); i++) {
      sourceBuilder.append(separator);
      // Emit parameters in the more readable inline short form.
      emitParameter(sourceBuilder, method, i);
      separator = ", ";
    }
    sourceBuilder.append(") ");
  }

  private void emitParameter(SourceBuilder sourceBuilder, MethodLike expression, int i) {
    Variable parameter = expression.getParameters().get(i);

    if (parameter == expression.getJsVarargsParameter()) {
      sourceBuilder.append("...");
    }
    if (!expression.getDescriptor().getEnclosingTypeDescriptor().isRaw()) {
      sourceBuilder.append(
          "/** " + closureTypesGenerator.getJsDocForParameter(expression, i) + " */ ");
    }
    sourceBuilder.emitWithMapping(
        // Only map parameters if they are named.
        AstUtils.removeUnnamedSourcePosition(parameter.getSourcePosition()),
        () -> sourceBuilder.append(getUniqueNameForVariable(parameter)));
  }
}
