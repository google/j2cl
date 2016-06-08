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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.processors.Visitable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A reference to a type.
 *
 * <p>
 * This class is mostly a bag of precomputed properties, and the details of how those properties are
 * created lives at in several creation functions in TypeDescriptors.
 *
 * <p>
 * A couple of properties are lazily calculated via the TypeDescriptorFactory and
 * MethodDescriptorFactory interfaces, since eagerly calculating them would lead to infinite loops
 * of TypeDescriptor creation.
 */
@Visitable
public class TypeDescriptor extends Node implements Comparable<TypeDescriptor>, HasJsName {

  public static Interner<TypeDescriptor> interner;

  private static Interner<TypeDescriptor> getInterner() {
    if (interner == null) {
      interner = Interners.newWeakInterner();
    }
    return interner;
  }

  private static TypeDescriptor intern(TypeDescriptor typeDescriptor) {
    // Run interning through a central function so that debugging has an opportunity to inspect
    // all of them.
    TypeDescriptor internedTypeDescriptor = getInterner().intern(typeDescriptor);
    return internedTypeDescriptor;
  }

  /**
   * Builder for a TypeDescriptor.
   */
  public static class Builder {

    public static Builder from(final TypeDescriptor typeDescriptor) {
      Builder builder = new Builder();
      TypeDescriptor newTypeDescriptor = builder.newTypeDescriptor;

      newTypeDescriptor.binaryName = typeDescriptor.getBinaryName();
      newTypeDescriptor.classComponents = typeDescriptor.getClassComponents();
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
      newTypeDescriptor.interfacesTypeDescriptorsFactory =
          new TypeDescriptorsFactory() {
            @Override
            public List<TypeDescriptor> create() {
              return typeDescriptor.getInterfacesTypeDescriptors();
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
      newTypeDescriptor.jsName = typeDescriptor.getJsName();
      newTypeDescriptor.jsNamespace = typeDescriptor.getJsNamespace();
      newTypeDescriptor.leafTypeDescriptor = typeDescriptor.getLeafTypeDescriptor();
      newTypeDescriptor.packageComponents = typeDescriptor.getPackageComponents();
      newTypeDescriptor.packageName = typeDescriptor.getPackageName();
      newTypeDescriptor.rawTypeDescriptorFactory =
          new TypeDescriptorFactory() {
            @Override
            public TypeDescriptor create() {
              return typeDescriptor.getRawTypeDescriptor();
            }
          };
      newTypeDescriptor.simpleName = typeDescriptor.getSimpleName();
      newTypeDescriptor.sourceName = typeDescriptor.getSourceName();
      newTypeDescriptor.isOrSubclassesJsConstructorClass =
          typeDescriptor.isOrSubclassesJsConstructorClass();
      newTypeDescriptor.superTypeDescriptorFactory =
          new TypeDescriptorFactory() {
            @Override
            public TypeDescriptor create() {
              return typeDescriptor.getSuperTypeDescriptor();
            }
          };
      newTypeDescriptor.unionedTypeDescriptors = typeDescriptor.getUnionedTypeDescriptors();
      newTypeDescriptor.typeArgumentDescriptors = typeDescriptor.getTypeArgumentDescriptors();
      newTypeDescriptor.visibility = typeDescriptor.getVisibility();

      return builder;
    }

    private TypeDescriptor newTypeDescriptor = new TypeDescriptor();

    public TypeDescriptor build() {
      return TypeDescriptor.intern(newTypeDescriptor);
    }

    public Builder setBinaryName(String binaryName) {
      newTypeDescriptor.binaryName = binaryName;
      return this;
    }

    public Builder setClassComponents(List<String> classComponents) {
      newTypeDescriptor.classComponents = classComponents;
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

    public Builder setInterfacesTypeDescriptorsFactory(
        TypeDescriptorsFactory interfacesTypeDescriptorsFactory) {
      newTypeDescriptor.interfacesTypeDescriptorsFactory = interfacesTypeDescriptorsFactory;
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

    public Builder setJsName(String jsName) {
      newTypeDescriptor.jsName = jsName;
      return this;
    }

    public Builder setJsNamespace(String jsNamespace) {
      newTypeDescriptor.jsNamespace = jsNamespace;
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

    public Builder setIsOrSubclassesJsConstructorClass(boolean isOrSubclassesJsConstructorClass) {
      newTypeDescriptor.isOrSubclassesJsConstructorClass = isOrSubclassesJsConstructorClass;
      return this;
    }

    public Builder setSuperTypeDescriptorFactory(TypeDescriptorFactory superTypeDescriptorFactory) {
      newTypeDescriptor.superTypeDescriptorFactory = superTypeDescriptorFactory;
      return this;
    }

    public Builder setTypeArgumentDescriptors(List<TypeDescriptor> typeArgumentDescriptors) {
      newTypeDescriptor.typeArgumentDescriptors = typeArgumentDescriptors;
      return this;
    }

    public Builder setUnionedTypeDescriptors(List<TypeDescriptor> unionedTypeDescriptors) {
      newTypeDescriptor.unionedTypeDescriptors = unionedTypeDescriptors;
      return this;
    }

    public Builder setVisibility(Visibility visibility) {
      newTypeDescriptor.visibility = visibility;
      return this;
    }
  }

  /**
   * Enables delayed MethodDescriptor creation.
   */
  public interface MethodDescriptorFactory {
    MethodDescriptor create();
  }

  /**
   * Enables delayed TypeDescriptor creation.
   */
  public interface TypeDescriptorFactory {
    TypeDescriptor create();
  }

  /**
   * Enables delayed TypeDescriptor creation.
   */
  public interface TypeDescriptorsFactory {
    List<TypeDescriptor> create();
  }


  private String binaryName;
  private List<String> classComponents = Collections.emptyList();
  private TypeDescriptor componentTypeDescriptor;
  private MethodDescriptorFactory concreteJsFunctionMethodDescriptorFactory;
  private int dimensions;
  private TypeDescriptorFactory enclosingTypeDescriptorFactory;
  private TypeDescriptorsFactory interfacesTypeDescriptorsFactory;
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
  private String jsName;
  private String jsNamespace;
  private TypeDescriptor leafTypeDescriptor;
  private List<String> packageComponents = Collections.emptyList();
  private String packageName;
  private TypeDescriptorFactory rawTypeDescriptorFactory;
  private String simpleName;
  private String sourceName;
  private boolean isOrSubclassesJsConstructorClass;
  private TypeDescriptorFactory superTypeDescriptorFactory;
  private List<TypeDescriptor> typeArgumentDescriptors = Collections.emptyList();
  private List<TypeDescriptor> unionedTypeDescriptors = Collections.emptyList();
  private Visibility visibility;

  private TypeDescriptor() {}

  @Override
  public Node accept(Processor processor) {
    return Visitor_TypeDescriptor.visit(processor, this);
  }

  @Override
  public int compareTo(TypeDescriptor that) {
    return getUniqueId().compareTo(that.getUniqueId());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TypeDescriptor) {
      return getUniqueId().equals(((TypeDescriptor) o).getUniqueId());
    }
    return false;
  }

  public boolean equalsIgnoreNullability(TypeDescriptor other) {
    return TypeDescriptors.toNullable(other).equals(TypeDescriptors.toNullable(this));
  }

  /**
   * Returns the unqualified binary name like "Outer$Inner".
   */
  public String getBinaryClassName() {
    String binaryClassName;
    {
      if (isPrimitive) {
        binaryClassName = "$" + simpleName;
      } else if (simpleName.equals("?")) {
        binaryClassName = "?";
      } else if (isTypeVariable) {
        // skip the top level class component for better output readability.
        List<String> nameComponents =
            new ArrayList<>(classComponents.subList(1, classComponents.size()));

        // move the prefix in the simple name to the class name to avoid collisions between method-
        // level and class-level type variable and avoid variable name starts with a number.
        // concat class components to avoid collisions between type variables in inner/outer class.
        // use '_' instead of '$' because '$' is not allowed in template variable name in closure.
        nameComponents.set(
            nameComponents.size() - 1, simpleName.substring(simpleName.indexOf('_') + 1));
        String prefix = simpleName.substring(0, simpleName.indexOf('_') + 1);

        binaryClassName = prefix + Joiner.on('_').join(nameComponents);
      } else if (isArray) {
        String arraySuffix = Strings.repeat("[]", dimensions);
        binaryClassName = leafTypeDescriptor.getBinaryClassName() + arraySuffix;
      } else {
        binaryClassName = Joiner.on('$').join(classComponents);
      }
    }
    return binaryClassName;
  }

  /**
   * Returns the fully package qualified binary name like "com.google.common.Outer$Inner".
   */
  public String getBinaryName() {
    return binaryName;
  }

  /**
   * Returns a list of Strings representing the current type's simple name and enclosing type simple
   * names. For example for "com.google.foo.Outer" the class components are ["Outer"] and for
   * "com.google.foo.Outer.Inner" the class components are ["Outer", "Inner"].
   */
  public List<String> getClassComponents() {
    return classComponents;
  }

  public TypeDescriptor getComponentTypeDescriptor() {
    return componentTypeDescriptor;
  }

  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    if (concreteJsFunctionMethodDescriptorFactory == null) {
      return null;
    }
    return concreteJsFunctionMethodDescriptorFactory.create();
  }

  public int getDimensions() {
    return dimensions;
  }

  public TypeDescriptor getEnclosingTypeDescriptor() {
    if (enclosingTypeDescriptorFactory == null) {
      return null;
    }
    return enclosingTypeDescriptorFactory.create();
  }

  public Set<TypeDescriptor> getAllTypeVariables() {
    Set<TypeDescriptor> typeVariables = new LinkedHashSet<>();
    getAllTypeVariables(this, typeVariables);
    return typeVariables;
  }

  private static void getAllTypeVariables(
      TypeDescriptor typeDescriptor, Set<TypeDescriptor> typeVariables) {
    if (typeDescriptor.isTypeVariable()) {
      typeVariables.add(typeDescriptor);
    }
    for (TypeDescriptor typeArgumentTypeDescriptor : typeDescriptor.getTypeArgumentDescriptors()) {
      getAllTypeVariables(typeArgumentTypeDescriptor, typeVariables);
    }
  }

  private static boolean startsWithNumber(String string) {
    if (Strings.isNullOrEmpty(string)) {
      return false;
    }
    char firstChar = string.charAt(0);
    return firstChar >= '0' && firstChar <= '9';
  }

  public ImmutableList<TypeDescriptor> getInterfacesTypeDescriptors() {
    if (interfacesTypeDescriptorsFactory == null) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(interfacesTypeDescriptorsFactory.create());
  }

  public MethodDescriptor getJsFunctionMethodDescriptor() {
    if (jsFunctionMethodDescriptorFactory == null) {
      return null;
    }
    return jsFunctionMethodDescriptorFactory.create();
  }

  @Override
  public String getJsName() {
    return jsName;
  }

  @Override
  public String getJsNamespace() {
    return jsNamespace;
  }

  public TypeDescriptor getLeafTypeDescriptor() {
    return leafTypeDescriptor;
  }

  public List<String> getPackageComponents() {
    return packageComponents;
  }

  /**
   * Returns the fully package qualified name like "com.google.common".
   */
  public String getPackageName() {
    return packageName;
  }

  public String getQualifiedName() {
    String effectiveSimpleName = jsName == null ? simpleName : jsName;
    String effectivePrefix = jsNamespace;
    TypeDescriptor enclosingTypeDescriptor = getEnclosingTypeDescriptor();
    if (effectivePrefix == null && enclosingTypeDescriptor != null) {

      if (enclosingTypeDescriptor.isNative) {
        // When there is a type nested within a native type, it's important not to generate a name
        // like "Array.1" (like would happen if the outer native type was claiming to be native
        // Array and the nested type was anonymous) since this is almost guaranteed to collide
        // with other people also creating nested classes within a native type that claims to be
        // native Array.
        effectivePrefix = enclosingTypeDescriptor.getSourceName();
      } else {
        effectivePrefix = enclosingTypeDescriptor.getQualifiedName();
      }
    }
    if (effectivePrefix == null) {
      effectivePrefix = packageName;
    }

    if (JsInteropUtils.isGlobal(effectivePrefix)) {
      return effectiveSimpleName;
    }
    // If the user opted in to declareLegacyNamespaces, then JSCompiler will complain when seeing
    // namespaces like "foo.bar.Baz.4". Prefix anonymous numbered classes with a string to make
    // JSCompiler happy.
    if (startsWithNumber(effectiveSimpleName)) {
      effectiveSimpleName = "$Anonymous" + effectiveSimpleName;
    }
    return effectivePrefix + "." + effectiveSimpleName;
  }

  /**
   * Returns the erasure type (see definition of erasure type at
   * http://help.eclipse.org/luna/index.jsp) with an empty type arguments list.
   */
  public TypeDescriptor getRawTypeDescriptor() {
    if (rawTypeDescriptorFactory == null) {
      return null;
    }
    return rawTypeDescriptorFactory.create();
  }

  /**
   * Returns the unqualified and unenclosed simple name like "Inner".
   */
  public String getSimpleName() {
    return simpleName;
  }

  /**
   * Returns the fully package qualified source name like "com.google.common.Outer.Inner".
   */
  public String getSourceName() {
    return sourceName;
  }

  public TypeDescriptor getSuperTypeDescriptor() {
    if (superTypeDescriptorFactory == null) {
      return null;
    }
    return superTypeDescriptorFactory.create();
  }

  public List<TypeDescriptor> getTypeArgumentDescriptors() {
    return typeArgumentDescriptors;
  }

  public List<TypeDescriptor> getUnionedTypeDescriptors() {
    return unionedTypeDescriptors;
  }

  public String getUniqueId() {
    String uniqueId;

    if (isArray) {
      uniqueId = "(" + leafTypeDescriptor.getUniqueId() + ")" + Strings.repeat("[]", dimensions);
    } else if (isUnion) {
      uniqueId = TypeDescriptors.createUnionBinaryName(unionedTypeDescriptors);
    } else if (isTypeVariable) {
      uniqueId = binaryName;
    } else {
      uniqueId = binaryName + TypeDescriptor.createTypeArgumentsUniqueId(typeArgumentDescriptors);
    }
    return (isNullable ? "?" : "!") + uniqueId;
  }

  private static String createTypeArgumentsUniqueId(List<TypeDescriptor> typeArgumentDescriptors) {
    if (typeArgumentDescriptors == null || typeArgumentDescriptors.isEmpty()) {
      return "";
    }
    return String.format(
        "<%s>",
        Joiner.on(", ")
            .join(
                Lists.transform(
                    typeArgumentDescriptors,
                    new Function<TypeDescriptor, String>() {
                      @Override
                      public String apply(TypeDescriptor typeDescriptor) {
                        return typeDescriptor.getUniqueId();
                      }
                    })));
  }

  public Visibility getVisibility() {
    return visibility;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getUniqueId());
  }

  /**
   * Returns whether the described type is an array.
   */
  public boolean isArray() {
    return isArray;
  }

  public boolean isEnumOrSubclass() {
    return isEnumOrSubclass;
  }

  public boolean isExtern() {
    return isExtern;
  }

  public boolean isGlobal() {
    return isGlobal;
  }

  public boolean isInstanceMemberClass() {
    return isInstanceMemberClass;
  }

  public boolean isInstanceNestedClass() {
    return isInstanceNestedClass;
  }

  public boolean isInterface() {
    return isInterface;
  }

  public boolean isJsFunctionImplementation() {
    return isJsFunctionImplementation;
  }

  public boolean isJsFunctionInterface() {
    return isJsFunction;
  }

  public boolean isJsType() {
    return isJsType;
  }

  /**
   * Returns whether the described type is a nested type (i.e. it is defined inside the body of some
   * enclosing type) but is not a member type because it's location in the body is not in the
   * declaration scope of the enclosing type. For example:
   *
   * <code>
   * class Foo {
   *   void bar() {
   *     class Baz {}
   *   }
   * }
   * </code>
   *
   * or
   *
   * <code>
   * class Foo {
   *   void bar() {
   *     Comparable comparable = new Comparable() { ... }
   *   }
   * }
   * </code>
   */
  public boolean isLocal() {
    return isLocal;
  }

  public boolean isNative() {
    return isNative;
  }

  public boolean isNullable() {
    return isNullable;
  }

  public boolean isParameterizedType() {
    return !getTypeArgumentDescriptors().isEmpty();
  }

  public boolean isPrimitive() {
    return isPrimitive;
  }

  /**
   * Returns whether this is a Raw reference. Raw references are not mangled in the output and thus
   * can be used to describe reference to JS apis.
   */
  public boolean isRaw() {
    return isRaw;
  }

  public boolean isRawType() {
    return isRawType;
  }

  public boolean isTypeVariable() {
    return isTypeVariable;
  }

  /**
   * Returns whether the described type is a union.
   */
  public boolean isUnion() {
    return isUnion;
  }

  public boolean isWildCard() {
    return isWildCard;
  }

  public boolean isOrSubclassesJsConstructorClass() {
    return isOrSubclassesJsConstructorClass;
  }

  @Override
  public String toString() {
    return getUniqueId();
  }
}
