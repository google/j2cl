/*
 * Copyright 2024 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.MoreCollectors.onlyElement;

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.JsMemberType;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.ThisOrSuperReference;
import com.google.j2cl.transpiler.ast.Type;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Optimize Kotlin companion.
 *
 * <p>A companion object that does not extend nor implement any other type will be optimized by
 *
 * <ul>
 *   <li>Converting methods from the companion object to static methods within the enclosing class.
 *   <li>Moving the unique instance field defined within the enclosing class to the companion
 *       object.
 * </ul>
 *
 * <p>This optimization aligns the Kotlin structure closer to the Java model and allows tools like
 * JsTrimmer to remove the companion class reducing the number of files going to JSC.
 *
 * <p>A note about companion object:
 *
 * <p>The following companion object
 *
 * <pre>ex: {@code
 * class EnclosingType {
 *   companion object {
 *     @JvmField var jvmField = 1
 *     var property = 1
 *     const val constantVal = 2
 *
 *     fun nonJvmStaticFun() {}
 *
 *     @JvmStatic
 *     fun jvmStaticFun(s: String): Int { ... }
 *
 *     @JvmStatic
 *     external fun nativeJvmStaticFun(s: String): Int
 *   }
 *  }
 * }</pre>
 *
 * is seen by J2CL as:
 *
 * <pre>{@code
 * class EnclosingType {
 *   class Companion {
 *     public int getProperty(int value) {
 *       return EnclosingType.property;
 *     }
 *
 *     public void setProperty(int value) {
 *       EnclosingType.property = value;
 *     }
 *
 *     public void nonJvmStaticFun() {}
 *
 *     public int jvmStaticFun(String s) { ... }
 *
 *     // for external JvmStatic function, kotlinc move the method on the enclosing type and make it
 *     // static. It also replace the original companion method by a forwarding method, calling the
 *     // native method on the enclosing type.
 *     public int nativeJvmStaticFun(String s) {
 *        return EnclosingType.nativeJvmStaticFun(s);
 *      }
 *   }
 *   // compile-time constant are moved on the enclosing types.
 *   public static final int constantVal = 2
 *
 *   // static field containing the unique instance of the Companion class
 *   public static final Companion Companion;
 *
 *   // JvmField property exposes a public field on the enclosing type. No getter/setter are created
 *   // on the companion class.
 *   public static int jvmField;
 *
 *   // For declared companion property, kotlinc defines a private backing field for the property on
 *   // the enclosing type and creates getter/setter methods on the companion. If the getter/setter
 *   // are annotated with `JvmStatic`, they follow the same rules than normal JvmStatic functions.
 *   private static int property;
 *
 *   static {
 *     Companion = new Companion();
 *     jvmField = 1;
 *     property = 1
 *   }
 *
 *   // for non-external JvmStatic function, kotlinc creates a static method on the enclosing type
 *   // forwarding the call to the companion instance method.
 *   static int foo(String s) {
 *     return Companion.foo(s);
 *   }
 *
 *   // For JNI compatibility, external companion method annotated with JvmStatic are moved to the
 *   // enclosing type.
 *   public native int nativeJvmStaticFun(String s);
 * }
 * }</pre>
 *
 * In J2CL, all companion object properties (including `const val` and `var`) of interfaces are
 * moved to the enclosing interface by the frontend. This differs from Kotlin/JVM, where only
 * immutable properties annotated with @JvmField are moved, and compile-time constants are
 * duplicated.
 */
public class OptimizeKotlinCompanions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitType(Type type) {
            if (type.getTypeDescriptor().isOptimizableKotlinCompanion()) {
              Type enclosingType = (Type) getParent();
              Field companionInstanceField =
                  enclosingType.getStaticFields().stream()
                      .filter(f -> isCompanionInstanceField(f.getDescriptor()))
                      .collect(onlyElement());

              rewriteThisReferenceInCompanion(type, companionInstanceField);
              moveCompanionMethodsToEnclosingType(enclosingType, type);
              moveCompanionInstanceFieldToCompanionClass(
                  enclosingType, type, companionInstanceField);
            }

            rewriteCompanionMethodCalls(type);
            rewriteCompanionInstanceFieldReferences(type);
          }
        });
  }

  /**
   * Mark all super method call as static dispatch. Then rewrite all `this` and `super` references
   * in the companion to reference to the unique instance field of the companion.
   */
  private static void rewriteThisReferenceInCompanion(
      Type companion, Field companionInstanceField) {
    companion.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessType(Type type) {
            // Do not recurse into nested types.
            return type == companion;
          }

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            if (methodCall.getQualifier() instanceof SuperReference) {
              // Mark the super method call as and explicit static dispatch since the SuperReference
              // will be rewritten below.
              return MethodCall.Builder.from(methodCall).setStaticDispatch(true).build();
            }
            return methodCall;
          }
        });

    companion.accept(
        // This intentionally traverses nested types to handle all companion 'this' references,
        // which can occur in nested classes.
        new AbstractRewriter() {
          @Override
          public Node rewriteThisOrSuperReference(ThisOrSuperReference thisOrSuperReference) {
            if (!thisOrSuperReference
                .getTypeDescriptor()
                .isSameBaseType(companion.getTypeDescriptor())) {
              return thisOrSuperReference;
            }

            return FieldAccess.Builder.from(companionInstanceField).build();
          }
        });
  }

  /**
   * Convert methods from the companion object to static methods within the enclosing class and add
   * a forwarding method on the companion that forwards the call to the newly introduced static
   * method.
   *
   * <p>We need to special case JvmStatic methods. When a companion method is annotated with
   * JvmStatic, KoltinC creates a static forwarding method on the enclosing type for Java interop
   * purpose.
   *
   * <pre>ex: {@code
   * class Foo {
   *   companion object {
   *     @JvmStatic fun foo(s: String): Int { ... }
   *   }
   * }
   * }</pre>
   *
   * is seen here as:
   *
   * <pre>{@code
   * class Foo {
   *   static final Companion Companion = new Companion()
   *   class Companion {
   *     int foo(String s) { ... }
   *   }
   *   static int foo(String s) {
   *     return Companion.foo(s);
   *   }
   * }
   * }</pre>
   *
   * <p>When the JvmStatic function is native, kotlinc move the method on the enclosing type (for
   * JNI compatibility) and creates the forwarding method on the companion. In that particular case
   * we do not have anything to do.
   *
   * <p>Custom `$isInstance` methods are moved to the enclosing type but no forwarding method is
   * created on the companion to avoid conflicting with the static `isInstance` method created
   * later.
   */
  private static void moveCompanionMethodsToEnclosingType(Type enclosingType, Type companion) {
    companion.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessType(Type type) {
            // Do not recurse into nested types.
            return type == companion;
          }

          @Override
          public Node rewriteMethod(Method method) {
            if (method.isConstructor()) {
              return method;
            }

            MethodDescriptor staticMethodDescriptor =
                getCorrespondingStaticMethodDescriptor(method.getDescriptor());

            if (staticMethodDescriptor.isNative()) {
              // This is a native JvmStatic method. The original method has already been moved to
              // the enclosing type and the companion method has been converted to a forwarding
              // method by kotlinc. Nothing needs to be done
              return method;
            }

            // If the companion method is non-native and annotated with JvmStatic, KoltinC created
            // a static forwarding method on the enclosing type. We will replace that method so
            // we need to remove it first.
            enclosingType
                .getMembers()
                .removeIf(m -> m.getDescriptor().equals(staticMethodDescriptor));

            enclosingType.addMember(
                Method.Builder.from(method).setMethodDescriptor(staticMethodDescriptor).build());

            // If the companion method is a custom `$isInstance` method, we do not want to create
            // a forwarding method on the companion to avoid conflicting with the static
            // `$isInstance` method added later.
            if (method.getDescriptor().isCustomIsInstanceMethod()) {
              return null;
            }

            // The companion method is replaced by a method calling the corresponding static method
            // in the enclosing type.
            return AstUtils.createForwardingMethod(
                method.getSourcePosition(),
                /* qualifier= */ null,
                method.getDescriptor(),
                staticMethodDescriptor,
                method.getJsDocDescription());
          }
        });
  }

  /**
   * Move the companion instance field from enclosing class to the companion class itself.
   *
   * <pre>ex: {@code
   * class Foo {
   *   companion object {
   *   }
   * }
   * }</pre>
   *
   * is seen here as:
   *
   * <pre>{@code
   * class Foo {
   *   static final Companion Companion
   *   static {
   *     Companion = new Companion()
   *   }
   *   class Companion {
   *   }
   * }
   * }</pre>
   *
   * after the pass the field is moved inside the companion class:
   *
   * <pre>{@code
   * class Foo {
   *   class Companion {
   *     static final Companion Companion
   *     static {
   *      Companion = new Companion()
   *     }
   *   }
   * }
   * }</pre>
   */
  private static void moveCompanionInstanceFieldToCompanionClass(
      Type enclosingType, Type companion, Field companionInstanceOnEnclosingType) {
    Field companionInstanceOnCompanion =
        Field.Builder.from(
                getCompanionInstanceFieldOnCompanion(
                    companionInstanceOnEnclosingType.getDescriptor()))
            .setSourcePosition(companionInstanceOnEnclosingType.getSourcePosition())
            .setNameSourcePosition(companionInstanceOnEnclosingType.getNameSourcePosition())
            .build();
    companion.addMember(companionInstanceOnCompanion);

    enclosingType.getMembers().remove(companionInstanceOnEnclosingType);

    // Koltinc initializes the companion field in a statement inside a static initializer block.
    // Find it and move it to the companion.
    List<Statement> initializers = new ArrayList<>();
    enclosingType
        .getStaticInitializerBlocks()
        .forEach(
            si ->
                si.accept(
                    new AbstractRewriter() {
                      @Override
                      public @Nullable Node rewriteExpressionStatement(
                          ExpressionStatement statement) {
                        if (!isFieldInitializer(
                            statement.getExpression(),
                            companionInstanceOnEnclosingType.getDescriptor())) {
                          return statement;
                        }

                        initializers.add(statement);

                        // Remove the statement from the enclosing type initializer.
                        return null;
                      }
                    }));
    Statement initializer = Iterables.getOnlyElement(initializers);

    companion.addStaticInitializerBlock(
        0,
        Block.newBuilder()
            .setSourcePosition(initializer.getSourcePosition())
            .addStatement(initializer)
            .build());
  }

  private static boolean isFieldInitializer(
      Expression expression, FieldDescriptor fieldDescriptor) {
    if (!(expression instanceof BinaryExpression)) {
      return false;
    }
    var binaryExpression = (BinaryExpression) expression;

    return binaryExpression.isSimpleAssignment()
        && binaryExpression.getLeftOperand() instanceof FieldAccess
        && fieldDescriptor.equals(((FieldAccess) binaryExpression.getLeftOperand()).getTarget());
  }

  /**
   * Convert calls to methods of optimized companion to calls to static methods within the enclosing
   * class.
   */
  private static void rewriteCompanionMethodCalls(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            if (!methodCall
                .getTarget()
                .getEnclosingTypeDescriptor()
                .isOptimizableKotlinCompanion()) {
              return methodCall;
            }

            Expression qualifier = methodCall.getQualifier();
            if (isCompanionFieldAccess(qualifier)) {
              // Accessing the companion instance field directly does not have side effects, so we
              // can safely remove the qualifier of the static method call.
              qualifier = null;
            }

            return MethodCall.Builder.from(methodCall)
                .setQualifier(qualifier)
                .setTarget(getCorrespondingStaticMethodDescriptor(methodCall.getTarget()))
                .build();
          }
        });
  }

  private static boolean isCompanionFieldAccess(Expression expression) {
    if (!(expression instanceof FieldAccess)) {
      return false;
    }
    FieldAccess fieldAccess = (FieldAccess) expression;

    return fieldAccess.getQualifier() == null && isCompanionInstanceField(fieldAccess.getTarget());
  }

  private static MethodDescriptor getCorrespondingStaticMethodDescriptor(
      MethodDescriptor companionMethod) {
    DeclaredTypeDescriptor companionEnclosingType =
        companionMethod.getEnclosingTypeDescriptor().getEnclosingTypeDescriptor();

    MethodDescriptor existingStaticMethodDescriptor = null;
    for (MethodDescriptor m : companionEnclosingType.getDeclaredMethodDescriptors()) {
      if (m.isStatic() && m.isSameSignature(companionMethod)) {
        checkState(existingStaticMethodDescriptor == null);
        existingStaticMethodDescriptor = m;
      }
    }

    if (existingStaticMethodDescriptor != null) {
      // There is an existing static method matching the companion method. It means that the
      // companion method is annotated with `JvmStatic`.
      // If a companion method is annotated with both @JvmStatic and JsInterop annotations, only the
      // static method created on the enclosing type retains JsInterop info.
      // Reuse this method to avoid losing JsInterop info.
      return existingStaticMethodDescriptor;
    }

    // The companion method is not annotated with `JvmStatic` create a new descriptor for the static
    // method.
    return companionMethod.transform(
        builder ->
            builder
                .setStatic(true)
                .setSynthetic(true)
                .setEnclosingTypeDescriptor(companionEnclosingType)
                .setOriginalJsInfo(
                    companionEnclosingType.isNative()
                            || companionEnclosingType.isJsFunctionInterface()
                        ? JsInfo.newBuilder()
                            .setJsMemberType(JsMemberType.NONE)
                            .setJsOverlay(true)
                            .build()
                        : JsInfo.NONE));
  }

  /**
   * Field keeping the unique instance of a optimized companion has been moved within the companion
   * class. Rewrite any remaining references to these field accordingly.
   */
  private static void rewriteCompanionInstanceFieldReferences(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            if (!isCompanionInstanceField(fieldAccess.getTarget())
                || !fieldAccess.getTarget().getTypeDescriptor().isOptimizableKotlinCompanion()) {
              return fieldAccess;
            }

            return FieldAccess.Builder.from(fieldAccess)
                .setTarget(getCompanionInstanceFieldOnCompanion(fieldAccess.getTarget()))
                .build();
          }
        });
  }

  private static boolean isCompanionInstanceField(FieldDescriptor f) {
    return f.getName().equals("Companion");
  }

  private static FieldDescriptor getCompanionInstanceFieldOnCompanion(
      FieldDescriptor companionInstanceField) {
    checkState(companionInstanceField.isDeclaration());

    return FieldDescriptor.Builder.from(companionInstanceField)
        .setEnclosingTypeDescriptor(
            (DeclaredTypeDescriptor) companionInstanceField.getTypeDescriptor())
        .setOriginalJsInfo(JsInfo.NONE)
        .build();
  }
}
