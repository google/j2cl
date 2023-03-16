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
package com.google.j2cl.transpiler.passes;


import com.google.common.base.Joiner;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.MemberReference;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.HashSet;
import java.util.Set;

/** Common code for different implementations of static intialization semantics. */
public abstract class ImplementStaticInitializationBase extends NormalizationPass {

  private final Set<String> privateMembersCalledFromOtherClasses = new HashSet<>();

  // TODO(b/187218486): Remove after adding a pass to convert constructors to static methods.
  private final boolean triggerClinitInConstructors;

  public ImplementStaticInitializationBase(boolean triggerClinitInConstructors) {
    this.triggerClinitInConstructors = triggerClinitInConstructors;
  }

  @Override
  public final void applyTo(CompilationUnit compilationUnit) {
    collectPrivateMemberReferences(compilationUnit);
    for (Type type : compilationUnit.getTypes()) {
      // Native types are not removed for the Wasm backend. Ignore them here.
      if (type.isNative()) {
        continue;
      }
      synthesizeClinitCallsInMethods(type);
      synthesizeSuperClinitCalls(type);
      // Apply the additional normalizations defined in subclasses.
      applyTo(type);
    }
  }

  /** Collect all private static methods and fields that are accessed from outside its class. */
  private void collectPrivateMemberReferences(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitMemberReference(MemberReference memberReference) {
            recordMemberReference(getCurrentType().getDeclaration(), memberReference.getTarget());
          }
        });
  }

  /** Records access to member {@code targetMember} from type {@code callerEnclosingType}. */
  private void recordMemberReference(
      TypeDeclaration callerEnclosingType, MemberDescriptor targetMember) {
    if (!targetMember.getVisibility().isPrivate()) {
      return;
    }

    if (!targetMember.isMemberOf(callerEnclosingType)) {
      privateMembersCalledFromOtherClasses.add(getUniqueIdentifier(targetMember));
    }
  }

  /** Returns a string that uniquely identifies a method. */
  private static String getUniqueIdentifier(MemberDescriptor memberDescriptor) {
    return Joiner.on("___")
        .join(
            memberDescriptor.getEnclosingTypeDescriptor().getQualifiedBinaryName(),
            memberDescriptor.getMangledName());
  }

  /** Add clinit calls to methods and (real js) constructors. */
  private void synthesizeClinitCallsInMethods(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Method rewriteMethod(Method method) {
            if (triggersClinit(method.getDescriptor(), type)) {
              return Method.Builder.from(method)
                  .addStatement(
                      0,
                      createClinitCallStatement(
                          method.getBody().getSourcePosition(),
                          method.getDescriptor().getEnclosingTypeDescriptor()))
                  .build();
            }
            return method;
          }
        });
  }

  /** Synthesize a static initializer block that calls the necessary super type clinits. */
  private static void synthesizeSuperClinitCalls(Type type) {
    Block.Builder staticInitializerBuilder =
        Block.newBuilder().setSourcePosition(type.getSourcePosition());

    if (implementsClinitMethod(type.getSuperTypeDescriptor())) {
      staticInitializerBuilder.addStatement(
          createClinitCallStatement(type.getSourcePosition(), type.getSuperTypeDescriptor()));
    }
    addRequiredSuperInterfacesClinitCalls(
        type.getSourcePosition(), type.getTypeDescriptor(), staticInitializerBuilder);

    Block staticInitiliazerBlock = staticInitializerBuilder.build();
    if (!staticInitiliazerBlock.isNoop()) {
      type.addStaticInitializerBlock(0, staticInitiliazerBlock);
    }
  }

  private static void addRequiredSuperInterfacesClinitCalls(
      SourcePosition sourcePosition,
      DeclaredTypeDescriptor typeDescriptor,
      Block.Builder staticInitializerBuilder) {
    for (DeclaredTypeDescriptor interfaceTypeDescriptor :
        typeDescriptor.getInterfaceTypeDescriptors()) {
      if (!implementsClinitMethod(interfaceTypeDescriptor)) {
        continue;
      }

      if (interfaceTypeDescriptor.getTypeDeclaration().declaresDefaultMethods()) {
        // The interface declares a default method; invoke its clinit which will initialize
        // the interface and all it superinterfaces that declare default methods.
        staticInitializerBuilder.addStatement(
            createClinitCallStatement(sourcePosition, interfaceTypeDescriptor));
        continue;
      }

      // The interface does not declare a default method so don't call its clinit; instead recurse
      // into its super interface hierarchy to invoke clinits of super interfaces that declare
      // default methods.
      addRequiredSuperInterfacesClinitCalls(
          sourcePosition, interfaceTypeDescriptor, staticInitializerBuilder);
    }
  }

  static Statement createClinitCallStatement(
      SourcePosition sourcePosition, DeclaredTypeDescriptor typeDescriptor) {
    return createClinitCallExpression(typeDescriptor).makeStatement(sourcePosition);
  }

  static Expression createClinitCallExpression(DeclaredTypeDescriptor typeDescriptor) {
    return MethodCall.Builder.from(typeDescriptor.getClinitMethodDescriptor()).build();
  }

  /**
   * Returns {@code true} if a class initialization (clinit) needs to be called when accessing this
   * member (i.e. calling it if if a method, or referencing it if it is a field)
   */
  // TODO(b/271900868): clinit should be called for instance JsOverlay methods for Wasm.
  boolean triggersClinit(MemberDescriptor memberDescriptor, Type enclosingType) {
    if (memberDescriptor.isNative()) {
      // Skip native members.
      return false;
    }

    if (memberDescriptor.isCompileTimeConstant()) {
      // Compile time constants do not trigger clinit.
      return false;
    }

    if (memberDescriptor.getVisibility().isPrivate()
        && !memberDescriptor.isJsMember()
        && !isCalledFromOtherClasses(memberDescriptor)) {
      // This is an effectively private member, which means that when this member is access clinit
      // is already guaranteed to have been called.
      return false;
    }

    return memberDescriptor.isStatic()
        || memberDescriptor.isJsConstructor()
        || (triggerClinitInConstructors
            && memberDescriptor.isConstructor()
            && !enclosingType.isOptimizedEnum())
        // non-private instance methods (except the synthetic ctor) of an optimized enum will
        // trigger clinit, since the constructor will not.
        || (triggersClinitInInstanceMethods(enclosingType) && isInstanceMethod(memberDescriptor));
  }

  private static boolean triggersClinitInInstanceMethods(Type type) {
    return type.isOptimizedEnum()
        || TypeDescriptors.isJavaLangEnum(type.getTypeDescriptor())
        || TypeDescriptors.isJavaLangObject(type.getTypeDescriptor());
  }

  private static boolean isInstanceMethod(MemberDescriptor memberDescriptor) {
    return memberDescriptor.isMethod()
        && memberDescriptor.isInstanceMember()
        && memberDescriptor.getOrigin() != MethodOrigin.SYNTHETIC_CTOR_FOR_CONSTRUCTOR;
  }

  private boolean isCalledFromOtherClasses(MemberDescriptor memberDescriptor) {
    return privateMembersCalledFromOtherClasses.contains(getUniqueIdentifier(memberDescriptor));
  }

  /** Returns {@code true} if the type implements the class initialization method. */
  private static boolean implementsClinitMethod(TypeDescriptor typeDescriptor) {
    return typeDescriptor != null
        && !typeDescriptor.isNative()
        && !typeDescriptor.isJsFunctionInterface();
  }
}
