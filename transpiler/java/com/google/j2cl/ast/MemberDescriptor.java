/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.ast;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.processors.common.Processor;
import javax.annotation.Nullable;

/** Abstract base class for member descriptors. */
@Visitable
public abstract class MemberDescriptor extends Node
    implements HasJsNameInfo, HasReadableDescription, HasUnusableByJsSuppression {

  /** Represents the origin of a specific member */
  public interface Origin {
    /** Returns the prefix to be used for mangling members */
    String getPrefix();
  }

  public abstract JsInfo getJsInfo();

  public abstract DeclaredTypeDescriptor getEnclosingTypeDescriptor();

  public abstract MemberDescriptor getDeclarationDescriptor();

  public abstract MemberDescriptor toRawMemberDescriptor();

  /** Returns true if {@code typeDescriptor} is the enclosing class of this member. */
  public boolean isMemberOf(DeclaredTypeDescriptor typeDescriptor) {
    return isMemberOf(typeDescriptor.getTypeDeclaration());
  }

  /** Returns true if {@code typeDeclaration} is the enclosing class of this member. */
  public boolean isMemberOf(TypeDeclaration typeDeclaration) {
    return getEnclosingTypeDescriptor().getTypeDeclaration().equals(typeDeclaration);
  }

  /** Returns true if {@code thatMemberDescriptor} is in the same type as this member. */
  public boolean inSameTypeAs(MemberDescriptor thatMemberDescriptor) {
    return thatMemberDescriptor.isMemberOf(getEnclosingTypeDescriptor());
  }

  /**
   * Returns true if both members are references/declaration of the same particular member in the
   * same enclosing class.
   */
  public abstract boolean isSameMember(MemberDescriptor thatMember);

  @Nullable
  public abstract String getName();

  public abstract Visibility getVisibility();

  @Override
  public abstract boolean isNative();

  public abstract boolean isStatic();

  public abstract boolean isFinal();

  public abstract boolean isPolymorphic();

  public abstract boolean isSynthetic();

  public abstract boolean isDeprecated();

  public abstract Origin getOrigin();

  public boolean isMethod() {
    return false;
  }

  public boolean isField() {
    return false;
  }

  public boolean isConstructor() {
    return false;
  }

  public boolean isDefaultMethod() {
    return false;
  }

  public boolean isEnumConstant() {
    return false;
  }

  public boolean isCompileTimeConstant() {
    return false;
  }

  public boolean isJsAsync() {
    return getJsInfo().isJsAsync();
  }

  public boolean isJsConstructor() {
    return getJsInfo().getJsMemberType() == JsMemberType.CONSTRUCTOR;
  }

  public boolean isJsPropertyGetter() {
    return getJsInfo().getJsMemberType() == JsMemberType.GETTER;
  }

  public boolean isJsPropertySetter() {
    return getJsInfo().getJsMemberType() == JsMemberType.SETTER;
  }

  public boolean isJsMethod() {
    return getJsInfo().getJsMemberType() == JsMemberType.METHOD;
  }

  public abstract boolean isJsFunction();

  public boolean isJsMember() {
    return getJsInfo().getJsMemberType() != JsMemberType.NONE;
  }

  public boolean isJsOverlay() {
    return getJsInfo().isJsOverlay();
  }

  public boolean isExtern() {
    return isNative() && hasExternNamespace();
  }

  private boolean hasExternNamespace() {
    checkArgument(isNative());
    // A native type descriptor is an extern if its namespace is the global namespace or if
    // it inherited the namespace from its (enclosing) extern type.
    return JsUtils.isGlobal(getJsNamespace())
        || (getEnclosingTypeDescriptor().getTypeDeclaration().isExtern()
            && getJsNamespace().equals(getEnclosingTypeDescriptor().getQualifiedJsName()));
  }

  /** Whether this member overrides a java.lang.Object method. */
  public boolean isOrOverridesJavaLangObjectMethod() {
    return false;
  }

  /** Returns whether the member can be referenced directly from JavaScript code. */
  public boolean canBeReferencedExternally() {
    if (getEnclosingTypeDescriptor().getTypeDeclaration().isAnonymous()) {
      // members of anonymous classes can not be referenced externally
      return false;
    }
    // TODO(b/36232076): There should be two methods isJsMember and isOrOverridesJsMember to
    // distinguish when a member is explicitly marked (to consider the member as
    // canBeReferencedExternally) and when the member inherits through overriding a JsMember.
    return isJsMember() || isJsFunction();
  }

  @Override
  public String getSimpleJsName() {
    String jsName = getJsInfo().getJsName();
    return jsName != null ? jsName : getJsInfo().getJsMemberType().computeJsName(this);
  }

  @Override
  public String getJsNamespace() {
    String jsNamespace = getJsInfo().getJsNamespace();
    return jsNamespace == null ? getEnclosingTypeDescriptor().getQualifiedJsName() : jsNamespace;
  }

  public boolean hasJsNamespace() {
    return getJsInfo().getJsNamespace() != null;
  }

  /** Returns a qualified binary name for the member. */
  public String getQualifiedBinaryName() {
    return String.format(
        "%s.%s", getEnclosingTypeDescriptor().getQualifiedBinaryName(), getBinaryName());
  }

  public abstract String getBinaryName();

  @Override
  public Node accept(Processor processor) {
    return Visitor_MemberDescriptor.visit(processor, this);
  }
}
