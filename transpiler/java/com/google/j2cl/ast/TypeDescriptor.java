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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.common.HasJsName;
import com.google.j2cl.ast.common.JsUtils;
import com.google.j2cl.common.Interner;
import com.google.j2cl.common.J2clUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A reference to a type.
 *
 * <p>This class is mostly a bag of precomputed properties, and the details of how those properties
 * are created lives at in several creation functions in TypeDescriptors.
 *
 * <p>A couple of properties are lazily calculated via the TypeDescriptorFactory and
 * MethodDescriptorFactory interfaces, since eagerly calculating them would lead to infinite loops
 * of TypeDescriptor creation.
 */
@Visitable
public class TypeDescriptor extends Node implements Comparable<TypeDescriptor>, HasJsName {

  public static Interner<TypeDescriptor> interner;

  private static Interner<TypeDescriptor> getInterner() {
    if (interner == null) {
      interner = new Interner<TypeDescriptor>();
    }
    return interner;
  }

  private static TypeDescriptor intern(TypeDescriptor typeDescriptor) {
    // Run interning through a central function so that debugging has an opportunity to inspect
    // all of them.
    TypeDescriptor internedTypeDescriptor = getInterner().intern(typeDescriptor);
    return internedTypeDescriptor;
  }

  /** Builder for a TypeDescriptor. */
  public static class Builder {

    public static Builder from(final TypeDescriptor typeDescriptor) {
      Builder builder = new Builder();
      TypeDescriptor newTypeDescriptor = builder.newTypeDescriptor;

      newTypeDescriptor.binaryName = typeDescriptor.getBinaryName();
      newTypeDescriptor.classComponents = typeDescriptor.getClassComponents();
      newTypeDescriptor.componentTypeDescriptor = typeDescriptor.getComponentTypeDescriptor();
      newTypeDescriptor.concreteJsFunctionMethodDescriptorFactory =
          new DescriptorFactory<MethodDescriptor>() {
            @Override
            public MethodDescriptor create(TypeDescriptor selfTypeDescriptor) {
              return typeDescriptor.getConcreteJsFunctionMethodDescriptor();
            }
          };
      newTypeDescriptor.dimensions = typeDescriptor.getDimensions();
      newTypeDescriptor.enclosingTypeDescriptorFactory =
          new DescriptorFactory<TypeDescriptor>() {
            @Override
            public TypeDescriptor create(TypeDescriptor selfTypeDescriptor) {
              return typeDescriptor.getEnclosingTypeDescriptor();
            }
          };
      newTypeDescriptor.interfacesTypeDescriptorsFactory =
          new DescriptorFactory<List<TypeDescriptor>>() {
            @Override
            public List<TypeDescriptor> create(TypeDescriptor selfTypeDescriptor) {
              return typeDescriptor.getInterfacesTypeDescriptors();
            }
          };
      newTypeDescriptor.isArray = typeDescriptor.isArray();
      newTypeDescriptor.isEnumOrSubclass = typeDescriptor.isEnumOrSubclass();
      newTypeDescriptor.isFinal = typeDescriptor.isFinal();
      newTypeDescriptor.isInstanceMemberClass = typeDescriptor.isInstanceMemberClass();
      newTypeDescriptor.isInstanceNestedClass = typeDescriptor.isInstanceNestedClass();
      newTypeDescriptor.isInterface = typeDescriptor.isInterface();
      newTypeDescriptor.isIntersection = typeDescriptor.isIntersection();
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
          new DescriptorFactory<MethodDescriptor>() {
            @Override
            public MethodDescriptor create(TypeDescriptor selfTypeDescriptor) {
              return typeDescriptor.getJsFunctionMethodDescriptor();
            }
          };
      newTypeDescriptor.jsName = typeDescriptor.getJsName();
      newTypeDescriptor.jsNamespace = typeDescriptor.getJsNamespace();
      newTypeDescriptor.leafTypeDescriptor = typeDescriptor.getLeafTypeDescriptor();
      newTypeDescriptor.packageComponents = typeDescriptor.getPackageComponents();
      newTypeDescriptor.packageName = typeDescriptor.getPackageName();
      newTypeDescriptor.rawTypeDescriptorFactory =
          new DescriptorFactory<TypeDescriptor>() {
            @Override
            public TypeDescriptor create(TypeDescriptor selfTypeDescriptor) {
              return typeDescriptor.getRawTypeDescriptor();
            }
          };
      newTypeDescriptor.simpleName = typeDescriptor.getSimpleName();
      newTypeDescriptor.sourceName = typeDescriptor.getSourceName();
      newTypeDescriptor.isOrSubclassesJsConstructorClass =
          typeDescriptor.isOrSubclassesJsConstructorClass();
      newTypeDescriptor.superTypeDescriptorFactory =
          new DescriptorFactory<TypeDescriptor>() {
            @Override
            public TypeDescriptor create(TypeDescriptor selfTypeDescriptor) {
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
      checkState(!newTypeDescriptor.isTypeVariable || newTypeDescriptor.isNullable);
      checkState(!newTypeDescriptor.isPrimitive || !newTypeDescriptor.isNullable);
      // TODO(tdeegan): Complete the precondition checks to make sure we are never buiding a
      // type descriptor that does not make sense.
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
        DescriptorFactory<MethodDescriptor> concreteJsFunctionMethodDescriptorFactory) {
      newTypeDescriptor.concreteJsFunctionMethodDescriptorFactory =
          concreteJsFunctionMethodDescriptorFactory;
      return this;
    }

    public Builder setDimensions(int dimensions) {
      newTypeDescriptor.dimensions = dimensions;
      return this;
    }

    public Builder setEnclosingTypeDescriptorFactory(
        DescriptorFactory<TypeDescriptor> enclosingTypeDescriptorFactory) {
      newTypeDescriptor.enclosingTypeDescriptorFactory = enclosingTypeDescriptorFactory;
      return this;
    }

    public Builder setInterfacesTypeDescriptorsFactory(
        DescriptorFactory<List<TypeDescriptor>> interfacesTypeDescriptorsFactory) {
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

    public Builder setIsFinal(boolean isFinal) {
      newTypeDescriptor.isFinal = isFinal;
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

    public Builder setIsIntersection(boolean isIntersection) {
      newTypeDescriptor.isIntersection = isIntersection;
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
        DescriptorFactory<MethodDescriptor> jsFunctionMethodDescriptorFactory) {
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

    public Builder setRawTypeDescriptorFactory(
        DescriptorFactory<TypeDescriptor> rawTypeDescriptorFactory) {
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

    public Builder setSuperTypeDescriptorFactory(
        DescriptorFactory<TypeDescriptor> superTypeDescriptorFactory) {
      newTypeDescriptor.superTypeDescriptorFactory = superTypeDescriptorFactory;
      return this;
    }

    public Builder setTypeArgumentDescriptors(Iterable<TypeDescriptor> typeArgumentDescriptors) {
      newTypeDescriptor.typeArgumentDescriptors = ImmutableList.copyOf(typeArgumentDescriptors);
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

    public Builder setDeclaredMethodDescriptorsFactory(
        DescriptorFactory<List<MethodDescriptor>> declaredMethodDescriptorsFactory) {
      newTypeDescriptor.declaredMethodDescriptorsFactory = declaredMethodDescriptorsFactory;
      return this;
    }
  }

  /**
   * References to some descriptors need to be deferred in some cases since it will cause infinite
   * loops.
   */
  public abstract static class DescriptorFactory<T> {
    private T cachedDescriptor;

    public T getOrCreate(TypeDescriptor selfTypeDescriptor) {
      if (cachedDescriptor == null) {
        cachedDescriptor = create(selfTypeDescriptor);
      }
      return cachedDescriptor;
    }

    protected abstract T create(TypeDescriptor selfTypeDescriptor);
  }

  private String binaryName;
  private List<String> classComponents = Collections.emptyList();
  private TypeDescriptor componentTypeDescriptor;
  private DescriptorFactory<MethodDescriptor> concreteJsFunctionMethodDescriptorFactory;
  private int dimensions;
  private DescriptorFactory<TypeDescriptor> enclosingTypeDescriptorFactory;
  private DescriptorFactory<List<TypeDescriptor>> interfacesTypeDescriptorsFactory;
  private boolean isArray;
  private boolean isEnumOrSubclass;
  private boolean isFinal;
  private boolean isInstanceMemberClass;
  private boolean isInstanceNestedClass;
  private boolean isInterface;
  private boolean isIntersection;
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
  private DescriptorFactory<MethodDescriptor> jsFunctionMethodDescriptorFactory;
  private String jsName;
  private String jsNamespace;
  private TypeDescriptor leafTypeDescriptor;
  private List<String> packageComponents = Collections.emptyList();
  private String packageName;
  private DescriptorFactory<TypeDescriptor> rawTypeDescriptorFactory;
  private String simpleName;
  private String sourceName;
  private boolean isOrSubclassesJsConstructorClass;
  private DescriptorFactory<TypeDescriptor> superTypeDescriptorFactory;
  private List<TypeDescriptor> typeArgumentDescriptors = Collections.emptyList();
  private List<TypeDescriptor> unionedTypeDescriptors = Collections.emptyList();
  private Visibility visibility;
  private DescriptorFactory<List<MethodDescriptor>> declaredMethodDescriptorsFactory;

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

  /** Returns the unqualified binary name like "Outer$Inner". */
  public String getBinaryClassName() {
    if (isPrimitive) {
      return "$" + simpleName;
    }
    if (simpleName.equals("?")) {
      return "?";
    }
    if (isTypeVariable) {
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

      return prefix + Joiner.on('_').join(nameComponents);
    }
    if (isArray) {
      String arraySuffix = Strings.repeat("[]", dimensions);
      return leafTypeDescriptor.getBinaryClassName() + arraySuffix;
    }
    return Joiner.on('$').join(classComponents);
  }

  /** Returns the fully package qualified binary name like "com.google.common.Outer$Inner". */
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
    return concreteJsFunctionMethodDescriptorFactory.getOrCreate(this);
  }

  public int getDimensions() {
    return dimensions;
  }

  public TypeDescriptor getEnclosingTypeDescriptor() {
    if (enclosingTypeDescriptorFactory == null) {
      return null;
    }
    return enclosingTypeDescriptorFactory.getOrCreate(this);
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
    checkArgument(!typeDescriptor.isUnion() || typeVariables.isEmpty());
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
    return ImmutableList.copyOf(interfacesTypeDescriptorsFactory.getOrCreate(this));
  }

  public List<TypeDescriptor> getIntersectedTypeDescriptors() {
    checkArgument(isIntersection());
    TypeDescriptor superType = getSuperTypeDescriptor();
    if (superType == TypeDescriptors.get().javaLangObject || superType == null) {
      return getInterfacesTypeDescriptors();
    }
    List<TypeDescriptor> types = new ArrayList<>(getInterfacesTypeDescriptors());
    types.add(superType);
    return types;
  }

  public MethodDescriptor getJsFunctionMethodDescriptor() {
    if (jsFunctionMethodDescriptorFactory == null) {
      return null;
    }
    return jsFunctionMethodDescriptorFactory.getOrCreate(this);
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

  /** Returns the fully package qualified name like "com.google.common". */
  public String getPackageName() {
    return packageName;
  }

  /** Returns the globally unique qualified name by which this type should be imported. */
  public String getQualifiedName() {
    // Externs have a simple and explicit name by which they are imported.
    if (isExtern()) {
      return getQualifiedNameForExtern();
    }
    TypeDescriptor enclosingTypeDescriptor = getEnclosingTypeDescriptor();

    String effectiveSimpleName = null;
    {
      // A proxy type must be imported with a modified name, otherwise it might collide with the
      // proxied type if it had the same name.
      if (isProxy()) {
        effectiveSimpleName = simpleName + "$$Proxy";
      }
      if (effectiveSimpleName == null) {
        effectiveSimpleName = jsName;
      }
      if (effectiveSimpleName == null) {
        effectiveSimpleName = simpleName;
      }
      // If the user opted in to declareLegacyNamespaces, then JSCompiler will complain when seeing
      // namespaces like "foo.bar.Baz.4". Prefix anonymous numbered classes with a string to make
      // JSCompiler happy.
      if (startsWithNumber(effectiveSimpleName)) {
        effectiveSimpleName = "$Anonymous" + effectiveSimpleName;
      }
    }

    String effectivePrefix = null;
    {
      // In non-native JsTypes the jsNamespace value customizes the namespace of the type being
      // translated, but in native JsTypes the jsNamespace stores the namespace of the native JS
      // class being proxied.
      if (!isProxy()) {
        effectivePrefix = jsNamespace;
      }
      if (JsUtils.isGlobal(jsNamespace)) {
        effectivePrefix = "";
      }
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
    }

    return Joiner.on(".")
        .skipNulls()
        .join(Strings.emptyToNull(effectivePrefix), effectiveSimpleName);
  }

  private String getQualifiedNameForExtern() {
    Preconditions.checkState(isExtern());

    String effectivePrefix = JsUtils.isGlobal(jsNamespace) ? JsUtils.GLOBAL_ALIAS : jsNamespace;
    String effectiveSimpleName = jsName == null ? simpleName : jsName;
    return Joiner.on(".")
        .skipNulls()
        .join(Strings.emptyToNull(effectivePrefix), effectiveSimpleName);
  }

  /**
   * Returns the qualified name of the native JS class being proxied.
   *
   * <p>This name is needed when generating a proxy forwarding output file.
   */
  public String getProxiedQualifiedName() {
    Preconditions.checkState(!isExtern());

    String effectiveSimpleName = jsName == null ? simpleName : jsName;
    String effectivePrefix = jsNamespace;
    if (JsUtils.isGlobal(jsNamespace)) {
      effectivePrefix = "";
    }
    if (effectivePrefix == null && getEnclosingTypeDescriptor() != null) {
      effectivePrefix = getEnclosingTypeDescriptor().getProxiedQualifiedName();
    }
    if (effectivePrefix == null) {
      effectivePrefix = packageName;
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
    return rawTypeDescriptorFactory.getOrCreate(this);
  }

  /** Returns the unqualified and unenclosed simple name like "Inner". */
  public String getSimpleName() {
    return simpleName;
  }

  /** Returns the fully package qualified source name like "com.google.common.Outer.Inner". */
  public String getSourceName() {
    return sourceName;
  }

  public TypeDescriptor getSuperTypeDescriptor() {
    if (superTypeDescriptorFactory == null) {
      return null;
    }
    return superTypeDescriptorFactory.getOrCreate(this);
  }

  public List<TypeDescriptor> getTypeArgumentDescriptors() {
    return typeArgumentDescriptors;
  }

  public List<TypeDescriptor> getUnionedTypeDescriptors() {
    return unionedTypeDescriptors;
  }

  /** A unique string for a give type. Used for interning. */
  public String getUniqueId() {
    String prefix = isNullable ? "?" : "!";
    if (isArray) {
      String leaf = leafTypeDescriptor.getUniqueId();
      return prefix + "(" + leaf + ")" + Strings.repeat("[]", dimensions);
    }
    return prefix
        + binaryName
        + TypeDescriptor.createTypeArgumentsUniqueId(typeArgumentDescriptors);
  }

  private static String createTypeArgumentsUniqueId(List<TypeDescriptor> typeArgumentDescriptors) {
    if (typeArgumentDescriptors == null || typeArgumentDescriptors.isEmpty()) {
      return "";
    }
    return J2clUtils.format(
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

  /** Returns whether the described type is an array. */
  public boolean isArray() {
    return isArray;
  }

  public boolean isEnumOrSubclass() {
    return isEnumOrSubclass;
  }

  public boolean isExtern() {
    return JsUtils.isGlobal(getJsNamespace()) && isNative();
  }

  public boolean isFinal() {
    return isFinal;
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

  public boolean isIntersection() {
    return isIntersection;
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
   * <p><code> class Foo { void bar() { class Baz {} } } </code>
   *
   * <p>or
   *
   * <p><code> class Foo { void bar() { Comparable comparable = new Comparable() { ... } } } </code>
   */
  public boolean isLocal() {
    return isLocal;
  }

  public boolean isNative() {
    return isNative;
  }

  private boolean isProxy() {
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

  /** Returns whether the described type is a union. */
  public boolean isUnion() {
    return isUnion;
  }

  public boolean isWildCard() {
    return isWildCard;
  }

  public boolean isOrSubclassesJsConstructorClass() {
    return isOrSubclassesJsConstructorClass;
  }

  /**
   * The list of methods declared in the type from the JDT. Note: this does not include methods we
   * synthesize and add to the type like bridge methods.
   */
  public List<MethodDescriptor> getDeclaredMethodDescriptors() {
    if (declaredMethodDescriptorsFactory == null) {
      return Collections.emptyList();
    }
    return declaredMethodDescriptorsFactory.getOrCreate(this);
  }

  // Used to cache the results of getAllMethods()
  private List<MethodDescriptor> allMethods = null;

  /**
   * The list of all methods available on a given type. TODO: update this code to handle package
   * private override rules.
   */
  public List<MethodDescriptor> getAllMethods() {
    if (allMethods != null) {
      return allMethods;
    }
    Map<String, MethodDescriptor> methodsBySignature = Maps.newHashMap();
    // Add methods declared in the type itself.
    updateMethodsBySignature(methodsBySignature, getDeclaredMethodDescriptors());
    // Recursively add methods that appear on super types
    if (getSuperTypeDescriptor() != null) {
      updateMethodsBySignature(methodsBySignature, getSuperTypeDescriptor().getAllMethods());
    }
    // Add methods that appear on super interfaces.
    for (TypeDescriptor interfaceType : getInterfacesTypeDescriptors()) {
      updateMethodsBySignature(methodsBySignature, interfaceType.getAllMethods());
    }
    allMethods = Lists.newArrayList(methodsBySignature.values());
    return allMethods;
  }

  private static void updateMethodsBySignature(
      Map<String, MethodDescriptor> methodsBySignature, List<MethodDescriptor> methodDescriptors) {
    for (MethodDescriptor declaredMethod : methodDescriptors) {
      String methodSignature = declaredMethod.getMethodSignature();
      if (methodsBySignature.containsKey(methodSignature)) {
        // We already found a method with this signature.
        if (declaredMethod.isDefault()
            && methodsBySignature
                .get(methodSignature)
                .getEnclosingClassTypeDescriptor()
                .isInterface()) {
          // Allow the method to be replaced.
        }
        continue;
      }
      methodsBySignature.put(methodSignature, declaredMethod);
    }
  }

  @Override
  public String toString() {
    return getUniqueId();
  }
}
