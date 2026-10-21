/*
 * Copyright 2026 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.transpiler.ast.J2ktAstUtils.getWebAsyncAnnotation;
import static com.google.j2cl.transpiler.ast.J2ktAstUtils.isWebAsync;
import static com.google.j2cl.transpiler.ast.J2ktAstUtils.isWebAsyncOverride;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Annotation;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SynchronizedStatement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.TryStatement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeLiteral;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import java.util.ArrayList;
import java.util.List;

/**
 * Normalizes @WebAsync and @DbAsync methods for J2KT:
 *
 * <ul>
 *   <li>Marks @WebAsync method definitions as suspend functions (adding @JsIgnore on public methods
 *       of non-native @JsTypes).
 *   <li>Synthesizes a bridge method taking an InterruptionToken parameter and returning a
 *       ListenableFuture when {@code generateFuture = true}.
 *   <li>Rewrites @WebAsync and AsyncAwait call sites to target their suspend or Suspension
 *       counterparts, and lowers synchronized blocks/methods inside suspend functions to
 *       Suspension.lock/unlock.
 * </ul>
 */
public class ImplementWebAsyncMethodsJ2kt extends NormalizationPass {

  private static final String JS_IGNORE_ANNOTATION_NAME = "jsinterop.annotations.JsIgnore";
  private static final String OBJECTIVE_C_KMP_METHOD_ANNOTATION_NAME =
      "com.google.j2objc.annotations.ObjectiveCKmpMethod";
  private static final String ASYNC_AWAIT_CLASS_NAME =
      "com.google.calendar.v2a.shared.async.AsyncAwait";

  private static final String LISTENABLE_FUTURE_CLASS_NAME =
      "com.google.common.util.concurrent.ListenableFuture";

  private static final TypeVariable LISTENABLE_FUTURE_TYPE_VARIABLE =
      TypeVariable.builder()
          .setName("V")
          .setUniqueKey(LISTENABLE_FUTURE_CLASS_NAME + ":V")
          .setUpperBoundTypeDescriptorFactory(
              () -> TypeDescriptors.get().javaLangObject.toNullable())
          .build();

  private static final DeclaredTypeDescriptor LISTENABLE_FUTURE_TYPE =
      TypeDeclaration.builder()
          .setKind(Kind.INTERFACE)
          .setQualifiedSourceName(LISTENABLE_FUTURE_CLASS_NAME)
          .setTypeParameterDescriptors(ImmutableList.of(LISTENABLE_FUTURE_TYPE_VARIABLE))
          .build()
          .toDescriptor()
          .toNonNullable();

  private static final String INTERRUPTION_TOKEN_CLASS_NAME =
      "com.google.calendar.v2a.shared.async.InterruptionToken";

  private static final DeclaredTypeDescriptor INTERRUPTION_TOKEN_TYPE =
      TypeDeclaration.builder()
          .setKind(Kind.CLASS)
          .setQualifiedSourceName(INTERRUPTION_TOKEN_CLASS_NAME)
          .build()
          .toDescriptor();

  private static final String SUSPENSION_CLASS_NAME =
      "com.google.calendar.v2a.shared.async.Suspension";

  private static final DeclaredTypeDescriptor SUSPENSION_TYPE =
      TypeDeclaration.builder()
          .setKind(Kind.CLASS)
          .setQualifiedSourceName(SUSPENSION_CLASS_NAME)
          .build()
          .toDescriptor();

  private static final String ASYNC_RUNNABLE_CLASS_NAME =
      "com.google.calendar.v2a.shared.async.AsyncRunnable";

  private static final DeclaredTypeDescriptor ASYNC_RUNNABLE_TYPE =
      TypeDeclaration.builder()
          .setKind(Kind.INTERFACE)
          .setQualifiedSourceName(ASYNC_RUNNABLE_CLASS_NAME)
          .setFunctionalInterface(true)
          .setDeclaredMethodDescriptorsFactory(
              typeDeclaration ->
                  ImmutableList.of(
                      MethodDescriptor.builder()
                          .setEnclosingTypeDescriptor(typeDeclaration.toDescriptor())
                          .setName("run")
                          .setReturnTypeDescriptor(PrimitiveTypes.VOID)
                          .setSuspendFunction(true)
                          .setAbstract(true)
                          .build()))
          .setSingleAbstractMethodDescriptorFactory(
              typeDeclaration -> typeDeclaration.getDeclaredMethodDescriptors().getFirst())
          .build()
          .toDescriptor()
          .toNonNullable();

  private static final String ASYNC_CALLABLE_CLASS_NAME =
      "com.google.calendar.v2a.shared.async.AsyncCallable";

  private static final TypeVariable ASYNC_CALLABLE_TYPE_VARIABLE =
      TypeVariable.builder()
          .setName("V")
          .setUniqueKey(ASYNC_CALLABLE_CLASS_NAME + ":V")
          .setUpperBoundTypeDescriptorFactory(
              () -> TypeDescriptors.get().javaLangObject.toNullable())
          .build();

  private static final DeclaredTypeDescriptor ASYNC_CALLABLE_TYPE =
      TypeDeclaration.builder()
          .setKind(Kind.INTERFACE)
          .setQualifiedSourceName(ASYNC_CALLABLE_CLASS_NAME)
          .setFunctionalInterface(true)
          .setTypeParameterDescriptors(ImmutableList.of(ASYNC_CALLABLE_TYPE_VARIABLE))
          .setDeclaredMethodDescriptorsFactory(
              typeDeclaration ->
                  ImmutableList.of(
                      MethodDescriptor.builder()
                          .setEnclosingTypeDescriptor(typeDeclaration.toDescriptor())
                          .setName("call")
                          .setReturnTypeDescriptor(ASYNC_CALLABLE_TYPE_VARIABLE)
                          .setSuspendFunction(true)
                          .setAbstract(true)
                          .build()))
          .setSingleAbstractMethodDescriptorFactory(
              typeDeclaration -> typeDeclaration.getDeclaredMethodDescriptors().getFirst())
          .build()
          .toDescriptor()
          .toNonNullable();

  private static final String HIDDEN_FROM_OBJC_CLASS_NAME =
      "com.google.j2kt.annotations.HiddenFromObjC";

  private static final boolean EXPERIMENTAL_WEBASYNC_COROUTINES =
      Boolean.getBoolean(
          "com.google.j2cl.transpiler.backend.kotlin.experimentalWebAsyncCoroutines");

  private final boolean enabled;

  public ImplementWebAsyncMethodsJ2kt() {
    this(EXPERIMENTAL_WEBASYNC_COROUTINES);
  }

  public ImplementWebAsyncMethodsJ2kt(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    if (!enabled) {
      return;
    }

    // 1. Transform method definitions in all types (including anonymous inner classes) and
    // synthesize bridge methods.
    compilationUnit.streamTypes().forEach(this::transformType);

    // 2. Rewrite call sites across the compilation unit.
    compilationUnit.accept(
        new AbstractRewriter() {
          private boolean isInsideWebAsync() {
            MethodLike enclosingMethod = (MethodLike) getParent(MethodLike.class::isInstance);
            return enclosingMethod != null
                && (isWebAsync(enclosingMethod.getDescriptor())
                    || enclosingMethod.getDescriptor().isSuspendFunction());
          }

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor target = methodCall.getTarget();
            boolean isWebAsyncTarget = isWebAsync(target);
            if (isWebAsyncTarget && !isInsideWebAsync()) {
              return methodCall;
            }
            if (target
                .getEnclosingTypeDescriptor()
                .getQualifiedSourceName()
                .equals(ASYNC_AWAIT_CLASS_NAME)) {
              return methodCall.toBuilder()
                  .setTarget(getSuspensionMethodDescriptor(target))
                  .build();
            }
            if (isWebAsyncTarget && !target.isSuspendFunction()) {
              return methodCall.toBuilder().setTarget(getSuspendMethodDescriptor(target)).build();
            }
            return methodCall;
          }

          @Override
          public Node rewriteSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
            if (isInsideWebAsync()) {
              SourcePosition sourcePosition = synchronizedStatement.getSourcePosition();
              return Block.builder()
                  .setSourcePosition(sourcePosition)
                  .setStatements(
                      createSynchronizedStatements(
                          sourcePosition,
                          synchronizedStatement.getExpression(),
                          synchronizedStatement.getBody()))
                  .build();
            }

            return synchronizedStatement;
          }
        });
  }

  private static ImmutableList<Statement> createSynchronizedStatements(
      SourcePosition sourcePosition, Expression lockExpression, Block body) {
    ImmutableList.Builder<Statement> statements = ImmutableList.builder();
    Expression lockRef;
    if (lockExpression.isEffectivelyInvariant()) {
      lockRef = lockExpression;
    } else {
      Variable lockVariable =
          Variable.builder()
              .setFinal(true)
              .setName("$lock")
              .setTypeDescriptor(lockExpression.getTypeDescriptor())
              .build();
      statements.add(
          VariableDeclarationExpression.builder()
              .setVariable(lockVariable)
              .setInitializer(lockExpression)
              .build()
              .makeStatement(sourcePosition));
      lockRef = lockVariable.createReference();
    }

    MethodDescriptor lockMethod =
        MethodDescriptor.builder()
            .setStatic(true)
            .setEnclosingTypeDescriptor(SUSPENSION_TYPE)
            .setName("lock")
            .setSuspendFunction(true)
            .setParameterDescriptors(
                ParameterDescriptor.create(
                    "lock", TypeDescriptors.get().javaLangObject.toNullable()))
            .setReturnTypeDescriptor(PrimitiveTypes.VOID)
            .build();

    MethodDescriptor unlockMethod =
        lockMethod.toBuilder().setName("unlock").setSuspendFunction(false).build();

    statements.add(
        MethodCall.builderFrom(lockMethod)
            .setArguments(lockRef)
            .setSourcePosition(sourcePosition)
            .build()
            .makeStatement(sourcePosition));

    Block finallyBlock =
        Block.builder()
            .setSourcePosition(sourcePosition)
            .setStatements(
                MethodCall.builderFrom(unlockMethod)
                    .setArguments(lockRef.clone())
                    .setSourcePosition(sourcePosition)
                    .build()
                    .makeStatement(sourcePosition))
            .build();

    statements.add(
        TryStatement.builder()
            .setSourcePosition(sourcePosition)
            .setBody(body)
            .setFinallyBlock(finallyBlock)
            .build());

    return statements.build();
  }

  private void transformType(Type type) {
    if (type.getDeclaration().getQualifiedSourceName().equals(ASYNC_AWAIT_CLASS_NAME)) {
      return;
    }

    for (Method method : type.getMethods()) {
      MethodDescriptor originalMd = method.getDescriptor();
      if (!isWebAsync(originalMd) || originalMd.isSuspendFunction()) {
        continue;
      }

      MethodDescriptor suspendMd = getSuspendMethodDescriptor(originalMd);
      Method.Builder suspendMethodBuilder =
          method.toBuilder()
              .setMethodDescriptor(suspendMd)
              .setForcedJavaOverride(method.isJavaOverride());

      if (originalMd.isSynchronized() && method.getBody() != null) {
        SourcePosition sourcePosition = method.getBody().getSourcePosition();
        Expression lockExpression =
            originalMd.isStatic()
                ? new TypeLiteral(sourcePosition, originalMd.getEnclosingTypeDescriptor())
                : new ThisReference(type.getTypeDescriptor());
        suspendMethodBuilder.setStatements(
            createSynchronizedStatements(sourcePosition, lockExpression, method.getBody()));
      }

      type.getMembers().set(type.getMembers().indexOf(method), suspendMethodBuilder.build());

      // If this method is an @Override of an @WebAsync method in a supertype,
      // it inherits the synthesized bridge method from the supertype.
      if (!isWebAsyncOverride(originalMd) && shouldGenerateFutureMethod(originalMd)) {
        type.addMember(synthesizeFutureMethod(type, method, suspendMd));
      }
    }
  }

  private static Method synthesizeFutureMethod(
      Type type, Method method, MethodDescriptor suspendMd) {
    MethodDescriptor md = method.getDescriptor();
    MethodDescriptor futureMd = getFutureMethodDescriptor(md);
    SourcePosition sourcePosition = method.getSourcePosition();
    List<Variable> futureParameters = new ArrayList<>(AstUtils.clone(method.getParameters()));
    ImmutableList<Expression> arguments =
        futureParameters.stream().map(Variable::createReference).collect(toImmutableList());
    // The trailing InterruptionToken parameter is unused in the coroutine implementation
    // (cancellation is propagated via ListenableFuture.cancel to the coroutine Job), but is kept
    // for signature compatibility with j2j-transformed Java callers and to disambiguate the
    // ListenableFuture bridge overload from the suspend method.
    futureParameters.add(
        Variable.builder()
            .setName("interruptionToken")
            .setTypeDescriptor(INTERRUPTION_TOKEN_TYPE.toNullable())
            .setParameter(true)
            .build());

    Expression targetQualifier = md.isStatic() ? null : new ThisReference(type.getTypeDescriptor());
    MethodCall suspendCall =
        MethodCall.builderFrom(suspendMd)
            .setQualifier(targetQualifier)
            .setArguments(arguments)
            .build();

    TypeDescriptor returnType = md.getReturnTypeDescriptor();
    boolean isVoid = TypeDescriptors.isPrimitiveVoid(returnType);
    DeclaredTypeDescriptor functionalType =
        isVoid
            ? ASYNC_RUNNABLE_TYPE
            : ASYNC_CALLABLE_TYPE.withTypeArguments(
                ImmutableList.of(returnType.isPrimitive() ? returnType.toBoxedType() : returnType));
    FunctionExpression lambda =
        FunctionExpression.builder()
            .setTypeDescriptor(functionalType)
            .setStatements(
                AstUtils.createReturnOrExpressionStatement(sourcePosition, suspendCall, returnType))
            .setSourcePosition(sourcePosition)
            .build();

    MethodDescriptor futureHelperMethod =
        MethodDescriptor.builder()
            .setStatic(true)
            .setEnclosingTypeDescriptor(SUSPENSION_TYPE)
            .setName(isVoid ? "futureVoid" : "future")
            .setParameterDescriptors(ParameterDescriptor.create("callable", functionalType))
            .setReturnTypeDescriptor(futureMd.getReturnTypeDescriptor())
            .build();

    MethodCall futureCall =
        MethodCall.builderFrom(futureHelperMethod)
            .setArguments(lambda)
            .setSourcePosition(sourcePosition)
            .build();

    return Method.builder()
        .setSourcePosition(sourcePosition)
        .setMethodDescriptor(futureMd)
        .setParameters(futureParameters)
        .setStatements(
            ReturnStatement.builder()
                .setExpression(futureCall)
                .setSourcePosition(sourcePosition)
                .build())
        .build();
  }

  private static boolean shouldGenerateFutureMethod(MethodDescriptor md) {
    Annotation annotation = getWebAsyncAnnotation(md);
    return annotation != null && annotation.getBooleanValue("generateFuture", false);
  }

  private static MethodDescriptor getSuspendMethodDescriptor(MethodDescriptor methodDescriptor) {
    // Strip any jsinterop or @ObjectiveCKmpMethod annotations from the suspend method
    ImmutableList.Builder<Annotation> annotationsBuilder =
        ImmutableList.<Annotation>builder()
            .addAll(
                methodDescriptor.getAnnotations().stream()
                    .filter(
                        a -> {
                          String name = a.getTypeDescriptor().getQualifiedSourceName();
                          return !name.startsWith("jsinterop.annotations.")
                              && !name.equals(OBJECTIVE_C_KMP_METHOD_ANNOTATION_NAME);
                        })
                    .iterator());
    if (canHaveJsIgnore(methodDescriptor)) {
      annotationsBuilder.add(Annotation.builderFrom(JS_IGNORE_ANNOTATION_NAME).build());
    }
    ImmutableList<Annotation> annotations = annotationsBuilder.build();

    return methodDescriptor.transform(
        builder ->
            builder
                .setSuspendFunction(true)
                .setSynchronized(false)
                .setOriginalJsInfo(JsInfo.NONE)
                .setAnnotations(annotations));
  }

  private static boolean canHaveJsIgnore(MethodDescriptor methodDescriptor) {
    DeclaredTypeDescriptor enclosingTypeDescriptor = methodDescriptor.getEnclosingTypeDescriptor();
    return enclosingTypeDescriptor.isJsType()
        && !enclosingTypeDescriptor.isNative()
        && methodDescriptor.getVisibility().isPublic();
  }

  /**
   * Returns true if the annotation specifies Swift/Objective-C naming, selectors, or adapters.
   * These annotations (@SwiftName, @ObjectiveCName, @ObjectiveCKmpMethod) are stripped from future
   * methods to prevent selector collision / adapter incompatibility in Swift and Objective-C.
   */
  private static boolean isSwiftOrObjCNameAnnotation(String name) {
    return name.equals("com.google.j2objc.annotations.SwiftName")
        || name.equals("com.google.j2objc.annotations.ObjectiveCName")
        || name.equals(OBJECTIVE_C_KMP_METHOD_ANNOTATION_NAME);
  }

  private static MethodDescriptor getSuspensionMethodDescriptor(MethodDescriptor methodDescriptor) {
    return methodDescriptor.transform(
        builder ->
            builder
                .setEnclosingTypeDescriptor(SUSPENSION_TYPE)
                .setSuspendFunction(isWebAsync(methodDescriptor)));
  }

  private static MethodDescriptor getFutureMethodDescriptor(MethodDescriptor methodDescriptor) {
    TypeDescriptor returnType = methodDescriptor.getReturnTypeDescriptor();
    TypeDescriptor valueTypeDescriptor;
    if (TypeDescriptors.isPrimitiveVoid(returnType)) {
      valueTypeDescriptor = TypeVariable.createWildcard();
    } else if (returnType.isPrimitive()) {
      valueTypeDescriptor = returnType.toBoxedType();
    } else {
      valueTypeDescriptor = returnType;
    }
    DeclaredTypeDescriptor futureReturnType =
        LISTENABLE_FUTURE_TYPE.withTypeArguments(ImmutableList.of(valueTypeDescriptor));

    List<Annotation> paramAnnotations =
        methodDescriptor.isOrOverridesJsMethod()
            ? ImmutableList.of(Annotation.builderFrom("jsinterop.annotations.JsOptional").build())
            : ImmutableList.of();
    ImmutableList<ParameterDescriptor> newParamDescriptors =
        ImmutableList.<ParameterDescriptor>builder()
            .addAll(
                methodDescriptor.getParameterDescriptors().stream()
                    .map(pd -> pd.toBuilder().setVarargs(false).build())
                    .iterator())
            .add(
                ParameterDescriptor.builder()
                    .setName("interruptionToken")
                    .setTypeDescriptor(INTERRUPTION_TOKEN_TYPE.toNullable())
                    .setAnnotations(paramAnnotations)
                    .build())
            .build();

    ImmutableList<Annotation> annotations =
        ImmutableList.<Annotation>builder()
            .addAll(
                methodDescriptor.getAnnotations().stream()
                    .filter(
                        a -> {
                          String name = a.getTypeDescriptor().getQualifiedSourceName();
                          return !isSwiftOrObjCNameAnnotation(name)
                              && !name.equals(HIDDEN_FROM_OBJC_CLASS_NAME)
                              && (!name.equals(JS_IGNORE_ANNOTATION_NAME)
                                  || canHaveJsIgnore(methodDescriptor));
                        })
                    .iterator())
            .add(Annotation.builderFrom(HIDDEN_FROM_OBJC_CLASS_NAME).build())
            .build();

    return methodDescriptor.toBuilder()
        .setSuspendFunction(false)
        .setSynchronized(false)
        .setAbstract(false)
        .setAnnotations(annotations)
        .setParameterDescriptors(newParamDescriptors)
        .setReturnTypeDescriptor(futureReturnType)
        .build();
  }
}
