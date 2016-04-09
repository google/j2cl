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
package com.google.j2cl.ast;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.processors.Visitable;

import java.util.List;

/**
 * A reference to a class.
 */
@Visitable
public class RegularTypeDescriptor extends TypeDescriptor {
  private final String binaryName;
  private final List<String> classComponents;
  private final String className;
  private final MethodDescriptorFactory concreteJsFunctionMethodDescriptorFactory;
  private final int dimensions;
  private final TypeDescriptorFactory enclosingTypeDescriptorFactory;
  private final boolean isArray;
  private final boolean isEnumOrSubclass;
  private final boolean isExtern;
  private final boolean isGlobal;
  private final boolean isInstanceMemberClass;
  private final boolean isInstanceNestedClass;
  private final boolean isInterface;
  private final boolean isJsFunction;
  private final boolean isJsFunctionImplementation;
  private final boolean isJsType;
  private final boolean isLocal;
  private final boolean isNative;
  private final boolean isPrimitive;
  private final boolean isRaw;
  private final boolean isRawType;
  private final boolean isTypeVariable;
  private final boolean isUnion;
  private final boolean isWildCard;
  private final MethodDescriptorFactory jsFunctionMethodDescriptorFactory;
  private final String jsTypeName;
  private final String jsTypeNamespace;
  private final List<String> packageComponents;
  private final String packageName;
  private final String qualifiedName;
  private final TypeDescriptorFactory rawTypeDescriptorFactory;
  private final String simpleName;
  private final String sourceName;
  private final boolean subclassesJsConstructorClass;
  private final TypeDescriptorFactory superTypeDescriptorFactory;
  private final String toString;
  private final List<TypeDescriptor> typeArgumentDescriptors;
  private final String uniqueId;
  private final Visibility visibility;

  RegularTypeDescriptor(
      String binaryName,
      List<String> classComponents,
      String className,
      MethodDescriptorFactory concreteJsFunctionMethodDescriptorFactory,
      int dimensions,
      TypeDescriptorFactory enclosingTypeDescriptorFactory,
      boolean isArray,
      boolean isEnumOrSubclass,
      boolean isExtern,
      boolean isGlobal,
      boolean isInstanceMemberClass,
      boolean isInstanceNestedClass,
      boolean isInterface,
      boolean isJsFunction,
      boolean isJsFunctionImplementation,
      boolean isJsType,
      boolean isLocal,
      boolean isNative,
      boolean isPrimitive,
      boolean isRaw,
      boolean isRawType,
      boolean isTypeVariable,
      boolean isUnion,
      boolean isWildCard,
      MethodDescriptorFactory jsFunctionMethodDescriptorFactory,
      String jsTypeName,
      String jsTypeNamespace,
      List<String> packageComponents,
      String packageName,
      String qualifiedName,
      TypeDescriptorFactory rawTypeDescriptorFactory,
      String simpleName,
      String sourceName,
      boolean subclassesJsConstructorClass,
      TypeDescriptorFactory superTypeDescriptorFactory,
      String toString,
      List<TypeDescriptor> typeArgumentDescriptors,
      String uniqueId,
      Visibility visibility) {
    this.binaryName = binaryName;
    this.classComponents = ImmutableList.copyOf(classComponents);
    this.className = className;
    this.concreteJsFunctionMethodDescriptorFactory = concreteJsFunctionMethodDescriptorFactory;
    this.dimensions = dimensions;
    this.enclosingTypeDescriptorFactory = enclosingTypeDescriptorFactory;
    this.isArray = isArray;
    this.isEnumOrSubclass = isEnumOrSubclass;
    this.isExtern = isExtern;
    this.isGlobal = isGlobal;
    this.isInstanceMemberClass = isInstanceMemberClass;
    this.isInstanceNestedClass = isInstanceNestedClass;
    this.isInterface = isInterface;
    this.isJsFunction = isJsFunction;
    this.isJsFunctionImplementation = isJsFunctionImplementation;
    this.isJsType = isJsType;
    this.isLocal = isLocal;
    this.isNative = isNative;
    this.isPrimitive = isPrimitive;
    this.isRaw = isRaw;
    this.isRawType = isRawType;
    this.isTypeVariable = isTypeVariable;
    this.isUnion = isUnion;
    this.isWildCard = isWildCard;
    this.jsFunctionMethodDescriptorFactory = jsFunctionMethodDescriptorFactory;
    this.jsTypeName = jsTypeName;
    this.jsTypeNamespace = jsTypeNamespace;
    this.packageComponents = ImmutableList.copyOf(packageComponents);
    this.packageName = packageName;
    this.qualifiedName = qualifiedName;
    this.rawTypeDescriptorFactory = rawTypeDescriptorFactory;
    this.simpleName = simpleName;
    this.sourceName = sourceName;
    this.subclassesJsConstructorClass = subclassesJsConstructorClass;
    this.superTypeDescriptorFactory = superTypeDescriptorFactory;
    this.toString = toString;
    this.typeArgumentDescriptors = ImmutableList.copyOf(typeArgumentDescriptors);
    this.uniqueId = uniqueId;
    this.visibility = visibility;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_RegularTypeDescriptor.visit(processor, this);
  }

  @Override
  public String getBinaryName() {
    return binaryName;
  }

  @Override
  public List<String> getClassComponents() {
    return classComponents;
  }

  @Override
  public String getClassName() {
    return className;
  }

  @Override
  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    return concreteJsFunctionMethodDescriptorFactory.create();
  }

  @Override
  public int getDimensions() {
    return dimensions;
  }

  @Override
  public TypeDescriptor getEnclosingTypeDescriptor() {
    return enclosingTypeDescriptorFactory.create();
  }

  @Override
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    return jsFunctionMethodDescriptorFactory.create();
  }

  @Override
  public String getJsName() {
    return jsTypeName;
  }

  @Override
  public String getJsNamespace() {
    return jsTypeNamespace;
  }

  @Override
  public List<String> getPackageComponents() {
    return packageComponents;
  }

  @Override
  public String getPackageName() {
    return packageName;
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public TypeDescriptor getRawTypeDescriptor() {
    return rawTypeDescriptorFactory.create();
  }

  @Override
  public String getSimpleName() {
    return simpleName;
  }

  @Override
  public String getSourceName() {
    return sourceName;
  }

  @Override
  public TypeDescriptor getSuperTypeDescriptor() {
    return superTypeDescriptorFactory.create();
  }

  @Override
  public List<TypeDescriptor> getTypeArgumentDescriptors() {
    return typeArgumentDescriptors;
  }

  @Override
  public String getUniqueId() {
    return uniqueId;
  }

  @Override
  public Visibility getVisibility() {
    return visibility;
  }

  @Override
  public boolean isArray() {
    return isArray;
  }

  @Override
  public boolean isEnumOrSubclass() {
    return isEnumOrSubclass;
  }

  @Override
  public boolean isExtern() {
    return isExtern;
  }

  @Override
  public boolean isGlobal() {
    return isGlobal;
  }

  @Override
  public boolean isInstanceMemberClass() {
    return isInstanceMemberClass;
  }

  @Override
  public boolean isInstanceNestedClass() {
    return isInstanceNestedClass;
  }

  @Override
  public boolean isInterface() {
    return isInterface;
  }

  @Override
  public boolean isJsFunctionImplementation() {
    return isJsFunctionImplementation;
  }

  @Override
  public boolean isJsFunctionInterface() {
    return isJsFunction;
  }

  @Override
  public boolean isJsType() {
    return isJsType;
  }

  @Override
  public boolean isLocal() {
    return isLocal;
  }

  @Override
  public boolean isNative() {
    return isNative;
  }

  @Override
  public boolean isParameterizedType() {
    return !getTypeArgumentDescriptors().isEmpty();
  }

  @Override
  public boolean isPrimitive() {
    return isPrimitive;
  }

  @Override
  public boolean isRaw() {
    return isRaw;
  }

  @Override
  public boolean isRawType() {
    return isRawType;
  }

  @Override
  public boolean isTypeVariable() {
    return isTypeVariable;
  }

  @Override
  public boolean isUnion() {
    return isUnion;
  }

  @Override
  public boolean isWildCard() {
    return isWildCard;
  }

  @Override
  public boolean subclassesJsConstructorClass() {
    return subclassesJsConstructorClass;
  }

  @Override
  public String toString() {
    return toString;
  }

  /**
   * Builder for a RegularTypeDescriptor.
   */
  public static class Builder {
    private String binaryName;
    private List<String> classComponents;
    private String className;
    private MethodDescriptorFactory concreteJsFunctionMethodDescriptorFactory;
    private int dimensions;
    private TypeDescriptorFactory enclosingTypeDescriptorFactory;
    private boolean isArray;
    private boolean isEnumOrSubclass;
    private boolean isExtern;
    private boolean isGlobal;
    private boolean isInstanceMemberClass;
    private boolean isInstanceNestedClass;
    private boolean isInterface;
    private boolean isJsFunction;
    private boolean isJsFunctionImplementation;
    private boolean isJsType;
    private boolean isLocal;
    private boolean isNative;
    private boolean isPrimitive;
    private boolean isRaw;
    private boolean isRawType;
    private boolean isTypeVariable;
    private boolean isUnion;
    private boolean isWildCard;
    private MethodDescriptorFactory jsFunctionMethodDescriptorFactory;
    private String jsTypeName;
    private String jsTypeNamespace;
    private List<String> packageComponents;
    private String packageName;
    private String qualifiedName;
    private TypeDescriptorFactory rawTypeDescriptorFactory;
    private String simpleName;
    private String sourceName;
    private boolean subclassesJsConstructorClass;
    private TypeDescriptorFactory superTypeDescriptorFactory;
    private String toString;
    private List<TypeDescriptor> typeArgumentDescriptors;
    private String uniqueId;
    private Visibility visibility;

    public static Builder from(final TypeDescriptor typeDescriptor) {
      Builder builder = new Builder();

      builder.binaryName = typeDescriptor.getBinaryName();
      builder.classComponents = typeDescriptor.getClassComponents();
      builder.className = typeDescriptor.getClassName();
      builder.concreteJsFunctionMethodDescriptorFactory =
          new MethodDescriptorFactory() {
            @Override
            public MethodDescriptor create() {
              return typeDescriptor.getConcreteJsFunctionMethodDescriptor();
            }
          };
      builder.dimensions = typeDescriptor.getDimensions();
      builder.enclosingTypeDescriptorFactory =
          new TypeDescriptorFactory() {
            @Override
            public TypeDescriptor create() {
              return typeDescriptor.getEnclosingTypeDescriptor();
            }
          };
      builder.isArray = typeDescriptor.isArray();
      builder.isEnumOrSubclass = typeDescriptor.isEnumOrSubclass();
      builder.isExtern = typeDescriptor.isExtern();
      builder.isGlobal = typeDescriptor.isGlobal();
      builder.isInstanceMemberClass = typeDescriptor.isInstanceMemberClass();
      builder.isInstanceNestedClass = typeDescriptor.isInstanceNestedClass();
      builder.isInterface = typeDescriptor.isInterface();
      builder.isJsFunction = typeDescriptor.isJsFunctionInterface();
      builder.isJsFunctionImplementation = typeDescriptor.isJsFunctionImplementation();
      builder.isJsType = typeDescriptor.isJsType();
      builder.isLocal = typeDescriptor.isLocal();
      builder.isNative = typeDescriptor.isNative();
      builder.isPrimitive = typeDescriptor.isPrimitive();
      builder.isRaw = typeDescriptor.isRaw();
      builder.isRawType = typeDescriptor.isRawType();
      builder.isTypeVariable = typeDescriptor.isTypeVariable();
      builder.isUnion = typeDescriptor.isUnion();
      builder.isWildCard = typeDescriptor.isWildCard();
      builder.jsFunctionMethodDescriptorFactory =
          new MethodDescriptorFactory() {
            @Override
            public MethodDescriptor create() {
              return typeDescriptor.getJsFunctionMethodDescriptor();
            }
          };
      builder.jsTypeName = typeDescriptor.getJsName();
      builder.jsTypeNamespace = typeDescriptor.getJsNamespace();
      builder.packageComponents = typeDescriptor.getPackageComponents();
      builder.packageName = typeDescriptor.getPackageName();
      builder.qualifiedName = typeDescriptor.getQualifiedName();
      builder.rawTypeDescriptorFactory =
          new TypeDescriptorFactory() {
            @Override
            public TypeDescriptor create() {
              return typeDescriptor.getRawTypeDescriptor();
            }
          };
      builder.simpleName = typeDescriptor.getSimpleName();
      builder.sourceName = typeDescriptor.getSourceName();
      builder.subclassesJsConstructorClass = typeDescriptor.subclassesJsConstructorClass();
      builder.superTypeDescriptorFactory =
          new TypeDescriptorFactory() {
            @Override
            public TypeDescriptor create() {
              return typeDescriptor.getSuperTypeDescriptor();
            }
          };
      builder.toString = typeDescriptor.toString();
      builder.typeArgumentDescriptors = typeDescriptor.getTypeArgumentDescriptors();
      builder.uniqueId = typeDescriptor.getUniqueId();
      builder.visibility = typeDescriptor.getVisibility();

      return builder;
    }

    public Builder setTypeArgumentDescriptors(List<TypeDescriptor> typeArgumentDescriptors) {
      this.typeArgumentDescriptors = typeArgumentDescriptors;
      return this;
    }

    public Builder setUniqueId(String uniqueId) {
      this.uniqueId = uniqueId;
      return this;
    }

    public RegularTypeDescriptor build() {
      return new RegularTypeDescriptor(
          binaryName,
          classComponents,
          className,
          concreteJsFunctionMethodDescriptorFactory,
          dimensions,
          enclosingTypeDescriptorFactory,
          isArray,
          isEnumOrSubclass,
          isExtern,
          isGlobal,
          isInstanceMemberClass,
          isInstanceNestedClass,
          isInterface,
          isJsFunction,
          isJsFunctionImplementation,
          isJsType,
          isLocal,
          isNative,
          isPrimitive,
          isRaw,
          isRawType,
          isTypeVariable,
          isUnion,
          isWildCard,
          jsFunctionMethodDescriptorFactory,
          jsTypeName,
          jsTypeNamespace,
          packageComponents,
          packageName,
          qualifiedName,
          rawTypeDescriptorFactory,
          simpleName,
          sourceName,
          subclassesJsConstructorClass,
          superTypeDescriptorFactory,
          toString,
          typeArgumentDescriptors,
          uniqueId,
          visibility);
    }
  }

  @Override
  public boolean isNullable() {
    return true;
  }

  @Override
  public NonNullableTypeDescriptor getNonNullable() {
    return NonNullableTypeDescriptor.create(this);
  }
}
