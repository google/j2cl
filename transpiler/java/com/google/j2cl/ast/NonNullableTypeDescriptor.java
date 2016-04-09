package com.google.j2cl.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

/**
 * Represents a non nullable type.
 */
public class NonNullableTypeDescriptor extends TypeDescriptor {
  TypeDescriptor underlyingTypeDescriptor;

  public static NonNullableTypeDescriptor create(TypeDescriptor underlyingTypeDescriptor) {
    return (NonNullableTypeDescriptor)
        TypeDescriptors.getInterner()
            .intern(new NonNullableTypeDescriptor(underlyingTypeDescriptor));
  }

  private NonNullableTypeDescriptor(TypeDescriptor underlyingTypeDescriptor) {
    this.underlyingTypeDescriptor = checkNotNull(underlyingTypeDescriptor);
    checkArgument(!(underlyingTypeDescriptor instanceof NonNullableTypeDescriptor));
  }

  public TypeDescriptor getUnderlyingTypeDescriptor() {
    return underlyingTypeDescriptor;
  }

  @Override
  public boolean isNullable() {
    return false;
  }

  @Override
  public String getBinaryName() {
    return underlyingTypeDescriptor.getBinaryName();
  }

  @Override
  public String getClassName() {
    return underlyingTypeDescriptor.getClassName();
  }

  @Override
  public String getSimpleName() {
    return underlyingTypeDescriptor.getSimpleName();
  }

  @Override
  public String getSourceName() {
    return underlyingTypeDescriptor.getSourceName();
  }

  @Override
  public String getPackageName() {
    return underlyingTypeDescriptor.getPackageName();
  }

  @Override
  public boolean isArray() {
    return underlyingTypeDescriptor.isArray();
  }

  @Override
  public boolean isRaw() {
    return underlyingTypeDescriptor.isRaw();
  }

  @Override
  public int getDimensions() {
    return underlyingTypeDescriptor.getDimensions();
  }

  @Override
  public boolean isParameterizedType() {
    return underlyingTypeDescriptor.isParameterizedType();
  }

  @Override
  public String getUniqueId() {
    return "!" + underlyingTypeDescriptor.getUniqueId();
  }

  @Override
  public TypeDescriptor getRawTypeDescriptor() {
    return underlyingTypeDescriptor.getRawTypeDescriptor();
  }

  @Override
  public List<TypeDescriptor> getTypeArgumentDescriptors() {
    return underlyingTypeDescriptor.getTypeArgumentDescriptors();
  }

  @Override
  public boolean isJsFunctionInterface() {
    return underlyingTypeDescriptor.isJsFunctionInterface();
  }

  @Override
  public boolean isJsFunctionImplementation() {
    return underlyingTypeDescriptor.isJsFunctionImplementation();
  }

  @Override
  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    return underlyingTypeDescriptor.getConcreteJsFunctionMethodDescriptor();
  }

  @Override
  public NonNullableTypeDescriptor getNonNullable() {
    return this;
  }

  @Override
  public List<String> getClassComponents() {
    return underlyingTypeDescriptor.getClassComponents();
  }

  @Override
  public TypeDescriptor getEnclosingTypeDescriptor() {
    return underlyingTypeDescriptor.getEnclosingTypeDescriptor();
  }

  @Override
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    return underlyingTypeDescriptor.getJsFunctionMethodDescriptor();
  }

  @Override
  public String getJsName() {
    return underlyingTypeDescriptor.getJsName();
  }

  @Override
  public String getJsNamespace() {
    return underlyingTypeDescriptor.getJsNamespace();
  }

  @Override
  public List<String> getPackageComponents() {
    return underlyingTypeDescriptor.getPackageComponents();
  }

  @Override
  public String getQualifiedName() {
    return underlyingTypeDescriptor.getQualifiedName();
  }

  @Override
  public TypeDescriptor getSuperTypeDescriptor() {
    return underlyingTypeDescriptor.getSuperTypeDescriptor();
  }

  @Override
  public Visibility getVisibility() {
    return underlyingTypeDescriptor.getVisibility();
  }

  @Override
  public boolean isEnumOrSubclass() {
    return underlyingTypeDescriptor.isEnumOrSubclass();
  }

  @Override
  public boolean isExtern() {
    return underlyingTypeDescriptor.isExtern();
  }

  @Override
  public boolean isGlobal() {
    return underlyingTypeDescriptor.isGlobal();
  }

  @Override
  public boolean isInstanceMemberClass() {
    return underlyingTypeDescriptor.isInstanceMemberClass();
  }

  @Override
  public boolean isInstanceNestedClass() {
    return underlyingTypeDescriptor.isInstanceMemberClass();
  }

  @Override
  public boolean isInterface() {
    return underlyingTypeDescriptor.isInterface();
  }

  @Override
  public boolean isJsType() {
    return underlyingTypeDescriptor.isJsType();
  }

  @Override
  public boolean isLocal() {
    return underlyingTypeDescriptor.isLocal();
  }

  @Override
  public boolean isNative() {
    return underlyingTypeDescriptor.isNative();
  }

  @Override
  public boolean isPrimitive() {
    return underlyingTypeDescriptor.isPrimitive();
  }

  @Override
  public boolean isRawType() {
    return underlyingTypeDescriptor.isRawType();
  }

  @Override
  public boolean isTypeVariable() {
    return underlyingTypeDescriptor.isTypeVariable();
  }

  @Override
  public boolean isUnion() {
    return underlyingTypeDescriptor.isUnion();
  }

  @Override
  public boolean isWildCard() {
    return underlyingTypeDescriptor.isWildCard();
  }

  @Override
  public boolean subclassesJsConstructorClass() {
    return underlyingTypeDescriptor.subclassesJsConstructorClass();
  }
}
