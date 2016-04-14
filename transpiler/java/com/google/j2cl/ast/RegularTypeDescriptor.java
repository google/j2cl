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
  private String binaryName;
  private List<String> classComponents;
  private String className;
  private TypeDescriptor componentTypeDescriptor;
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
  private boolean isNullable;
  private boolean isPrimitive;
  private boolean isRaw;
  private boolean isRawType;
  private boolean isTypeVariable;
  private boolean isUnion;
  private boolean isWildCard;
  private MethodDescriptorFactory jsFunctionMethodDescriptorFactory;
  private String jsTypeName;
  private String jsTypeNamespace;
  private TypeDescriptor leafTypeDescriptor;
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

  private RegularTypeDescriptor() {}

  RegularTypeDescriptor(
      String binaryName,
      List<String> classComponents,
      String className,
      TypeDescriptor componentTypeDescriptor,
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
      boolean isNullable,
      boolean isPrimitive,
      boolean isRaw,
      boolean isRawType,
      boolean isTypeVariable,
      boolean isUnion,
      boolean isWildCard,
      MethodDescriptorFactory jsFunctionMethodDescriptorFactory,
      String jsTypeName,
      String jsTypeNamespace,
      TypeDescriptor leafTypeDescriptor,
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
    this.classComponents = copyImmutableList(classComponents);
    this.className = className;
    this.componentTypeDescriptor = componentTypeDescriptor;
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
    this.isNullable = isNullable;
    this.isPrimitive = isPrimitive;
    this.isRaw = isRaw;
    this.isRawType = isRawType;
    this.isTypeVariable = isTypeVariable;
    this.isUnion = isUnion;
    this.isWildCard = isWildCard;
    this.jsFunctionMethodDescriptorFactory = jsFunctionMethodDescriptorFactory;
    this.jsTypeName = jsTypeName;
    this.jsTypeNamespace = jsTypeNamespace;
    this.leafTypeDescriptor = leafTypeDescriptor;
    this.packageComponents = copyImmutableList(packageComponents);
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
  public TypeDescriptor getComponentTypeDescriptor() {
    return componentTypeDescriptor;
  }

  @Override
  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    if (concreteJsFunctionMethodDescriptorFactory == null) {
      return null;
    }
    return concreteJsFunctionMethodDescriptorFactory.create();
  }

  @Override
  public int getDimensions() {
    return dimensions;
  }

  @Override
  public TypeDescriptor getEnclosingTypeDescriptor() {
    if (enclosingTypeDescriptorFactory == null) {
      return null;
    }
    return enclosingTypeDescriptorFactory.create();
  }

  @Override
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    if (jsFunctionMethodDescriptorFactory == null) {
      return null;
    }
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
  public TypeDescriptor getLeafTypeDescriptor() {
    return leafTypeDescriptor;
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
    if (rawTypeDescriptorFactory == null) {
      return null;
    }
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
    if (superTypeDescriptorFactory == null) {
      return null;
    }
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

    private RegularTypeDescriptor newTypeDescriptor = new RegularTypeDescriptor();

    public static Builder from(final TypeDescriptor typeDescriptor) {
      Builder builder = new Builder();
      RegularTypeDescriptor newTypeDescriptor = builder.newTypeDescriptor;

      newTypeDescriptor.binaryName = typeDescriptor.getBinaryName();
      newTypeDescriptor.classComponents = typeDescriptor.getClassComponents();
      newTypeDescriptor.className = typeDescriptor.getClassName();
      newTypeDescriptor.componentTypeDescriptor = typeDescriptor.getComponentTypeDescriptor();
      newTypeDescriptor.concreteJsFunctionMethodDescriptorFactory =
          new MethodDescriptorFactory() {
            @Override
            public MethodDescriptor create() {
              return typeDescriptor.getConcreteJsFunctionMethodDescriptor();
            }
          };
      newTypeDescriptor.dimensions = typeDescriptor.getDimensions();
      newTypeDescriptor.enclosingTypeDescriptorFactory =
          new TypeDescriptorFactory() {
            @Override
            public TypeDescriptor create() {
              return typeDescriptor.getEnclosingTypeDescriptor();
            }
          };
      newTypeDescriptor.isArray = typeDescriptor.isArray();
      newTypeDescriptor.isEnumOrSubclass = typeDescriptor.isEnumOrSubclass();
      newTypeDescriptor.isExtern = typeDescriptor.isExtern();
      newTypeDescriptor.isGlobal = typeDescriptor.isGlobal();
      newTypeDescriptor.isInstanceMemberClass = typeDescriptor.isInstanceMemberClass();
      newTypeDescriptor.isInstanceNestedClass = typeDescriptor.isInstanceNestedClass();
      newTypeDescriptor.isInterface = typeDescriptor.isInterface();
      newTypeDescriptor.isJsFunction = typeDescriptor.isJsFunctionInterface();
      newTypeDescriptor.isJsFunctionImplementation = typeDescriptor.isJsFunctionImplementation();
      newTypeDescriptor.isJsType = typeDescriptor.isJsType();
      newTypeDescriptor.isLocal = typeDescriptor.isLocal();
      newTypeDescriptor.isNative = typeDescriptor.isNative();
      newTypeDescriptor.isNullable = typeDescriptor.isNullable();
      newTypeDescriptor.isPrimitive = typeDescriptor.isPrimitive();
      newTypeDescriptor.isRaw = typeDescriptor.isRaw();
      newTypeDescriptor.isRawType = typeDescriptor.isRawType();
      newTypeDescriptor.isTypeVariable = typeDescriptor.isTypeVariable();
      newTypeDescriptor.isUnion = typeDescriptor.isUnion();
      newTypeDescriptor.isWildCard = typeDescriptor.isWildCard();
      newTypeDescriptor.jsFunctionMethodDescriptorFactory =
          new MethodDescriptorFactory() {
            @Override
            public MethodDescriptor create() {
              return typeDescriptor.getJsFunctionMethodDescriptor();
            }
          };
      newTypeDescriptor.jsTypeName = typeDescriptor.getJsName();
      newTypeDescriptor.jsTypeNamespace = typeDescriptor.getJsNamespace();
      newTypeDescriptor.leafTypeDescriptor = typeDescriptor.getLeafTypeDescriptor();
      newTypeDescriptor.packageComponents = typeDescriptor.getPackageComponents();
      newTypeDescriptor.packageName = typeDescriptor.getPackageName();
      newTypeDescriptor.qualifiedName = typeDescriptor.getQualifiedName();
      newTypeDescriptor.rawTypeDescriptorFactory =
          new TypeDescriptorFactory() {
            @Override
            public TypeDescriptor create() {
              return typeDescriptor.getRawTypeDescriptor();
            }
          };
      newTypeDescriptor.simpleName = typeDescriptor.getSimpleName();
      newTypeDescriptor.sourceName = typeDescriptor.getSourceName();
      newTypeDescriptor.subclassesJsConstructorClass =
          typeDescriptor.subclassesJsConstructorClass();
      newTypeDescriptor.superTypeDescriptorFactory =
          new TypeDescriptorFactory() {
            @Override
            public TypeDescriptor create() {
              return typeDescriptor.getSuperTypeDescriptor();
            }
          };
      newTypeDescriptor.toString = typeDescriptor.toString();
      newTypeDescriptor.typeArgumentDescriptors = typeDescriptor.getTypeArgumentDescriptors();
      newTypeDescriptor.uniqueId = typeDescriptor.getUniqueId();
      newTypeDescriptor.visibility = typeDescriptor.getVisibility();

      return builder;
    }

    public Builder setBinaryName(String binaryName) {
      newTypeDescriptor.binaryName = binaryName;
      return this;
    }

    public Builder setClassComponents(List<String> classComponents) {
      newTypeDescriptor.classComponents = classComponents;
      return this;
    }

    public Builder setClassName(String className) {
      newTypeDescriptor.className = className;
      return this;
    }

    public Builder setComponentTypeDescriptor(TypeDescriptor componentTypeDescriptor) {
      newTypeDescriptor.componentTypeDescriptor = componentTypeDescriptor;
      return this;
    }

    public Builder setConcreteJsFunctionMethodDescriptorFactory(
        MethodDescriptorFactory concreteJsFunctionMethodDescriptorFactory) {
      newTypeDescriptor.concreteJsFunctionMethodDescriptorFactory =
          concreteJsFunctionMethodDescriptorFactory;
      return this;
    }

    public Builder setDimensions(int dimensions) {
      newTypeDescriptor.dimensions = dimensions;
      return this;
    }

    public Builder setEnclosingTypeDescriptorFactory(
        TypeDescriptorFactory enclosingTypeDescriptorFactory) {
      newTypeDescriptor.enclosingTypeDescriptorFactory = enclosingTypeDescriptorFactory;
      return this;
    }

    public Builder setIsArray(boolean isArray) {
      newTypeDescriptor.isArray = isArray;
      return this;
    }

    public Builder setIsEnumOrSubclass(boolean isEnumOrSubclass) {
      newTypeDescriptor.isEnumOrSubclass = isEnumOrSubclass;
      return this;
    }

    public Builder setIsExtern(boolean isExtern) {
      newTypeDescriptor.isExtern = isExtern;
      return this;
    }

    public Builder setIsGlobal(boolean isGlobal) {
      newTypeDescriptor.isGlobal = isGlobal;
      return this;
    }

    public Builder setIsInstanceMemberClass(boolean isInstanceMemberClass) {
      newTypeDescriptor.isInstanceMemberClass = isInstanceMemberClass;
      return this;
    }

    public Builder setIsInstanceNestedClass(boolean isInstanceNestedClass) {
      newTypeDescriptor.isInstanceNestedClass = isInstanceNestedClass;
      return this;
    }

    public Builder setIsInterface(boolean isInterface) {
      newTypeDescriptor.isInterface = isInterface;
      return this;
    }

    public Builder setIsJsFunction(boolean isJsFunction) {
      newTypeDescriptor.isJsFunction = isJsFunction;
      return this;
    }

    public Builder setIsJsFunctionImplementation(boolean isJsFunctionImplementation) {
      newTypeDescriptor.isJsFunctionImplementation = isJsFunctionImplementation;
      return this;
    }

    public Builder setIsJsType(boolean isJsType) {
      newTypeDescriptor.isJsType = isJsType;
      return this;
    }

    public Builder setIsLocal(boolean isLocal) {
      newTypeDescriptor.isLocal = isLocal;
      return this;
    }

    public Builder setIsNative(boolean isNative) {
      newTypeDescriptor.isNative = isNative;
      return this;
    }

    public Builder setIsNullable(boolean isNullable) {
      newTypeDescriptor.isNullable = isNullable;
      return this;
    }

    public Builder setIsPrimitive(boolean isPrimitive) {
      newTypeDescriptor.isPrimitive = isPrimitive;
      return this;
    }

    public Builder setIsRaw(boolean isRaw) {
      newTypeDescriptor.isRaw = isRaw;
      return this;
    }

    public Builder setIsRawType(boolean isRawType) {
      newTypeDescriptor.isRawType = isRawType;
      return this;
    }

    public Builder setIsTypeVariable(boolean isTypeVariable) {
      newTypeDescriptor.isTypeVariable = isTypeVariable;
      return this;
    }

    public Builder setIsUnion(boolean isUnion) {
      newTypeDescriptor.isUnion = isUnion;
      return this;
    }

    public Builder setIsWildCard(boolean isWildCard) {
      newTypeDescriptor.isWildCard = isWildCard;
      return this;
    }

    public Builder setJsFunctionMethodDescriptorFactory(
        MethodDescriptorFactory jsFunctionMethodDescriptorFactory) {
      newTypeDescriptor.jsFunctionMethodDescriptorFactory = jsFunctionMethodDescriptorFactory;
      return this;
    }

    public Builder setJsTypeName(String jsTypeName) {
      newTypeDescriptor.jsTypeName = jsTypeName;
      return this;
    }

    public Builder setJsTypeNamespace(String jsTypeNamespace) {
      newTypeDescriptor.jsTypeNamespace = jsTypeNamespace;
      return this;
    }

    public Builder setLeafTypeDescriptor(TypeDescriptor leafTypeDescriptor) {
      newTypeDescriptor.leafTypeDescriptor = leafTypeDescriptor;
      return this;
    }

    public Builder setPackageComponents(List<String> packageComponents) {
      newTypeDescriptor.packageComponents = packageComponents;
      return this;
    }

    public Builder setPackageName(String packageName) {
      newTypeDescriptor.packageName = packageName;
      return this;
    }

    public Builder setQualifiedName(String qualifiedName) {
      newTypeDescriptor.qualifiedName = qualifiedName;
      return this;
    }

    public Builder setRawTypeDescriptorFactory(TypeDescriptorFactory rawTypeDescriptorFactory) {
      newTypeDescriptor.rawTypeDescriptorFactory = rawTypeDescriptorFactory;
      return this;
    }

    public Builder setSimpleName(String simpleName) {
      newTypeDescriptor.simpleName = simpleName;
      return this;
    }

    public Builder setSourceName(String sourceName) {
      newTypeDescriptor.sourceName = sourceName;
      return this;
    }

    public Builder setSubclassesJsConstructorClass(boolean subclassesJsConstructorClass) {
      newTypeDescriptor.subclassesJsConstructorClass = subclassesJsConstructorClass;
      return this;
    }

    public Builder setSuperTypeDescriptorFactory(TypeDescriptorFactory superTypeDescriptorFactory) {
      newTypeDescriptor.superTypeDescriptorFactory = superTypeDescriptorFactory;
      return this;
    }

    public Builder setToString(String toString) {
      newTypeDescriptor.toString = toString;
      return this;
    }

    public Builder setTypeArgumentDescriptors(List<TypeDescriptor> typeArgumentDescriptors) {
      newTypeDescriptor.typeArgumentDescriptors = typeArgumentDescriptors;
      return this;
    }

    public Builder setUniqueId(String uniqueId) {
      newTypeDescriptor.uniqueId = uniqueId;
      return this;
    }

    public Builder setVisibility(Visibility visibility) {
      newTypeDescriptor.visibility = visibility;
      return this;
    }

    public RegularTypeDescriptor build() {
      return newTypeDescriptor;
    }
  }

  @Override
  public boolean isNullable() {
    return isNullable;
  }

  private static List<String> copyImmutableList(List<String> classComponents) {
    if (classComponents == null) {
      return classComponents;
    }
    return ImmutableList.copyOf(classComponents);
  }
}
