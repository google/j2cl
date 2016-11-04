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
import static java.util.stream.Collectors.joining;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.common.HasJsNameInfo;
import com.google.j2cl.ast.common.HasReadableDescription;
import com.google.j2cl.ast.common.JsUtils;
import com.google.j2cl.common.Interner;
import com.google.j2cl.common.J2clUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
public class TypeDescriptor extends Node
    implements Comparable<TypeDescriptor>, HasJsNameInfo, HasReadableDescription {

  /** Builder for a TypeDescriptor. */
  public static class Builder {

    public static Builder from(final TypeDescriptor typeDescriptor) {
      Builder builder = new Builder();
      TypeDescriptor newTypeDescriptor = builder.newTypeDescriptor;

      newTypeDescriptor.uniqueKey = typeDescriptor.uniqueKey;
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
      newTypeDescriptor.interfaceTypeDescriptorsFactory =
          new DescriptorFactory<List<TypeDescriptor>>() {
            @Override
            public List<TypeDescriptor> create(TypeDescriptor selfTypeDescriptor) {
              return typeDescriptor.getInterfaceTypeDescriptors();
            }
          };
      newTypeDescriptor.isAbstract = typeDescriptor.isAbstract();
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
      newTypeDescriptor.isTypeVariable = typeDescriptor.isTypeVariable();
      newTypeDescriptor.isUnion = typeDescriptor.isUnion();
      newTypeDescriptor.isWildCardOrCapture = typeDescriptor.isWildCardOrCapture();
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
      newTypeDescriptor.packageName = typeDescriptor.getPackageName();
      newTypeDescriptor.rawTypeDescriptorFactory =
          new DescriptorFactory<TypeDescriptor>() {
            @Override
            public TypeDescriptor create(TypeDescriptor selfTypeDescriptor) {
              return typeDescriptor.getRawTypeDescriptor();
            }
          };
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
      newTypeDescriptor.declaredMethodDescriptorsFactory =
          new DescriptorFactory<Map<String, MethodDescriptor>>() {
            @Override
            public Map<String, MethodDescriptor> create(TypeDescriptor selfTypeDescriptor) {
              return typeDescriptor.getDeclaredMethodDescriptorsBySignature();
            }
          };

      return builder;
    }

    private final TypeDescriptor newTypeDescriptor = new TypeDescriptor();

    private static final ThreadLocal<Interner<TypeDescriptor>> interner = new ThreadLocal<>();

    private static Interner<TypeDescriptor> getInterner() {
      if (interner.get() == null) {
        interner.set(new Interner<>());
      }
      return interner.get();
    }

    public TypeDescriptor build() {
      checkState(!newTypeDescriptor.isTypeVariable || newTypeDescriptor.isNullable);
      checkState(!newTypeDescriptor.isPrimitive || !newTypeDescriptor.isNullable);
      
      // Default to binary name as the unique key.
      if (newTypeDescriptor.uniqueKey == null) {
        newTypeDescriptor.uniqueKey = newTypeDescriptor.getBinaryName();
      }

      // TODO(tdeegan): Complete the precondition checks to make sure we are never buiding a
      // type descriptor that does not make sense.
      return getInterner().intern(newTypeDescriptor);
    }

    public Builder setUniqueKey(String key) {
      newTypeDescriptor.uniqueKey = key;
      return this;
    }

    public Builder setBoundTypeDescriptorFactory(
        DescriptorFactory<TypeDescriptor> boundTypeDescriptorFactory) {
      newTypeDescriptor.boundTypeDescriptorFactory = boundTypeDescriptorFactory;
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

    public Builder setInterfaceTypeDescriptorsFactory(
        DescriptorFactory<List<TypeDescriptor>> interfaceTypeDescriptorsFactory) {
      newTypeDescriptor.interfaceTypeDescriptorsFactory = interfaceTypeDescriptorsFactory;
      return this;
    }

    public Builder setIsAbstract(boolean isAbstract) {
      newTypeDescriptor.isAbstract = isAbstract;
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

    public Builder setIsTypeVariable(boolean isTypeVariable) {
      newTypeDescriptor.isTypeVariable = isTypeVariable;
      return this;
    }

    public Builder setIsUnion(boolean isUnion) {
      newTypeDescriptor.isUnion = isUnion;
      return this;
    }

    public Builder setIsWildCardOrCapture(boolean isWildCardOrCapture) {
      newTypeDescriptor.isWildCardOrCapture = isWildCardOrCapture;
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

    public Builder setPackageName(String packageName) {
      newTypeDescriptor.packageName = packageName;
      return this;
    }

    public Builder setRawTypeDescriptorFactory(
        DescriptorFactory<TypeDescriptor> rawTypeDescriptorFactory) {
      newTypeDescriptor.rawTypeDescriptorFactory = rawTypeDescriptorFactory;
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
        DescriptorFactory<Map<String, MethodDescriptor>> declaredMethodDescriptorsFactory) {
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

  private String uniqueKey;
  private List<String> classComponents = Collections.emptyList();
  private TypeDescriptor componentTypeDescriptor;
  private DescriptorFactory<MethodDescriptor> concreteJsFunctionMethodDescriptorFactory;
  private int dimensions;
  private DescriptorFactory<TypeDescriptor> enclosingTypeDescriptorFactory;
  private DescriptorFactory<List<TypeDescriptor>> interfaceTypeDescriptorsFactory;
  private boolean isAbstract;
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
  private boolean isTypeVariable;
  private boolean isUnion;
  private boolean isWildCardOrCapture;
  private DescriptorFactory<MethodDescriptor> jsFunctionMethodDescriptorFactory;
  private String jsName;
  private String jsNamespace;
  private TypeDescriptor leafTypeDescriptor;
  private String packageName;
  private DescriptorFactory<TypeDescriptor> rawTypeDescriptorFactory;
  private boolean isOrSubclassesJsConstructorClass;
  private DescriptorFactory<TypeDescriptor> superTypeDescriptorFactory;
  private List<TypeDescriptor> typeArgumentDescriptors = Collections.emptyList();
  private List<TypeDescriptor> unionedTypeDescriptors = Collections.emptyList();
  private Visibility visibility;
  private DescriptorFactory<Map<String, MethodDescriptor>> declaredMethodDescriptorsFactory;
  private Map<String, MethodDescriptor> declaredMethodDescriptorssBySignature;
  private Map<String, MethodDescriptor> methodDescriptorssBySignature;
  private DescriptorFactory<TypeDescriptor> boundTypeDescriptorFactory;

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
    // TODO rename to getBinarySimpleName
    return Joiner.on('$').join(classComponents);
  }

  /** Returns the fully package qualified binary name like "com.google.common.Outer$Inner". */
  public String getBinaryName() {
    return Joiner.on(".").skipNulls().join(packageName, getBinaryClassName());
  }

  /** Returns the globally unique qualified name by which this type should be defined/imported. */
  public String getModuleName() {
    // TODO(goktug): fix the qualified name for primitives at construction phase.
    if (isPrimitive()) {
      return "vmbootstrap.primitives.$" + getSourceName();
    }

    if (isProxy()) {
      return getSourceName() + "$$Proxy";
    }

    return getQualifiedName();
  }

  public String getImplModuleName() {
    return isNative() || isExtern() ? getModuleName() : getModuleName() + "$impl";
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

  public boolean isSupertypeOf(TypeDescriptor that) {
    return that.getRawSuperTypesIncludingSelf().contains(this.getRawTypeDescriptor());
  }

  public boolean isSubtypeOf(TypeDescriptor that) {
    return getRawSuperTypesIncludingSelf().contains(that.getRawTypeDescriptor());
  }

  private Set<TypeDescriptor> allRawSupertypesIncludingSelf = null;

  private Set<TypeDescriptor> getRawSuperTypesIncludingSelf() {
    if (allRawSupertypesIncludingSelf == null) {
      allRawSupertypesIncludingSelf = new LinkedHashSet<>();
      allRawSupertypesIncludingSelf.add(getRawTypeDescriptor());
      if (getSuperTypeDescriptor() != null) {
        allRawSupertypesIncludingSelf.addAll(
            getSuperTypeDescriptor().getRawSuperTypesIncludingSelf());
      }
      for (TypeDescriptor interfaceTypeDescriptor : getInterfaceTypeDescriptors()) {
        allRawSupertypesIncludingSelf.addAll(
            interfaceTypeDescriptor.getRawSuperTypesIncludingSelf());
      }
    }
    return allRawSupertypesIncludingSelf;
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

  public ImmutableList<TypeDescriptor> getInterfaceTypeDescriptors() {
    if (interfaceTypeDescriptorsFactory == null) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(interfaceTypeDescriptorsFactory.getOrCreate(this));
  }

  public List<TypeDescriptor> getIntersectedTypeDescriptors() {
    checkState(isIntersection());
    TypeDescriptor superType = getSuperTypeDescriptor();
    // TODO(rluble): Reexamine this code after upgrading JDT to 4.7, where intersection types
    // are surfaced. Technically if one explicitly includes j.l.Object in the intersection type
    // then j.l.Object should be the first member of the intersection. In that case j2cl is not
    // consistent with Java.
    if (superType == TypeDescriptors.get().javaLangObject || superType == null) {
      return getInterfaceTypeDescriptors();
    }
    List<TypeDescriptor> types = new ArrayList<>();
    // First add the supertype and then the interfaces to be consistent with type erasure (JLS 4.6,
    // 13.1). Classes can only appear in leftmost position and the erasure is the leftmost bound.
    types.add(superType);
    types.addAll(getInterfaceTypeDescriptors());
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

  /** Returns the fully package qualified name like "com.google.common". */
  public String getPackageName() {
    return packageName;
  }

  /** Returns the qualified name of the type. */
  public String getQualifiedName() {
    String effectiveSimpleName = MoreObjects.firstNonNull(jsName, getSimpleName());

    String effectivePrefix = JsUtils.isGlobal(jsNamespace) ? "" : jsNamespace;

    if (effectivePrefix == null) {
      TypeDescriptor enclosingTypeDescriptor = getEnclosingTypeDescriptor();
      if (enclosingTypeDescriptor != null) {
        if (!isNative && enclosingTypeDescriptor.isNative) {
          // When there is a type nested within a native type, it's important not to generate a name
          // like "Array.1" (like would happen if the outer native type was claiming to be native
          // Array and the nested type was anonymous) since this is almost guaranteed to collide
          // with other people also creating nested classes within a native type that claims to be
          // native Array.
          effectivePrefix = enclosingTypeDescriptor.getSourceName();
        } else {
          // Use the parent namespace.
          effectivePrefix = enclosingTypeDescriptor.getQualifiedName();
        }
      } else {
        // Use the package namespace.
        effectivePrefix = packageName;
      }
    }

    return Joiner.on(".")
        .skipNulls()
        .join(Strings.emptyToNull(effectivePrefix), effectiveSimpleName);
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

  /** Returns the bound for a type variable. */
  public TypeDescriptor getBoundTypeDescriptor() {
    checkState(isTypeVariable || isWildCardOrCapture);
    if (boundTypeDescriptorFactory == null) {
      return null;
    }
    return boundTypeDescriptorFactory.getOrCreate(this);
  }

  /** Returns the unqualified and unenclosed simple name like "Inner". */
  public String getSimpleName() {
    String simpleName = Iterables.getLast(classComponents);
    // If the user opted in to declareLegacyNamespaces, then JSCompiler will complain when seeing
    // namespaces like "foo.bar.Baz.4". Prefix anonymous numbered classes with a string to make
    // JSCompiler happy.
    return startsWithNumber(simpleName) ? "$" + simpleName : simpleName;
  }

  private static boolean startsWithNumber(String string) {
    char firstChar = string.charAt(0);
    return firstChar >= '0' && firstChar <= '9';
  }

  /** Returns the fully package qualified source name like "com.google.common.Outer.Inner". */
  public String getSourceName() {
    return Joiner.on(".").skipNulls().join(packageName, Joiner.on(".").join(classComponents));
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
    return prefix + uniqueKey + TypeDescriptor.createTypeArgumentsUniqueId(typeArgumentDescriptors);
  }

  private static String createTypeArgumentsUniqueId(List<TypeDescriptor> typeArgumentDescriptors) {
    if (typeArgumentDescriptors == null || typeArgumentDescriptors.isEmpty()) {
      return "";
    }
    return J2clUtils.format(
        "<%s>",
        typeArgumentDescriptors.stream().map(TypeDescriptor::getUniqueId).collect(joining(", ")));
  }

  public Visibility getVisibility() {
    return visibility;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getUniqueId());
  }

  public boolean isAbstract() {
    return isAbstract;
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

  @Override
  public boolean isNative() {
    return isNative;
  }

  private boolean isProxy() {
    return isNative && !isExtern();
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

  public boolean isTypeVariable() {
    return isTypeVariable;
  }

  /** Returns whether the described type is a union. */
  public boolean isUnion() {
    return isUnion;
  }

  public boolean isWildCardOrCapture() {
    return isWildCardOrCapture;
  }

  public boolean isOrSubclassesJsConstructorClass() {
    return isOrSubclassesJsConstructorClass;
  }

  /**
   * The list of methods declared in the type from the JDT. Note: this does not include methods we
   * synthesize and add to the type like bridge methods.
   */
  private Map<String, MethodDescriptor> getDeclaredMethodDescriptorsBySignature() {
    if (declaredMethodDescriptorssBySignature == null) {
      if (declaredMethodDescriptorsFactory == null) {
        declaredMethodDescriptorssBySignature = ImmutableMap.of();
      } else {
        declaredMethodDescriptorssBySignature = declaredMethodDescriptorsFactory.getOrCreate(this);
      }
    }
    return declaredMethodDescriptorssBySignature;
  }

  /**
   * The list of methods in the type from the JDT. Note: this does not include methods we synthesize
   * and add to the type like bridge methods.
   */
  private Map<String, MethodDescriptor> getMethodDescriptorsBySignature() {
    // TODO(rluble): update this code to handle package private methods, bridges and verify that it
    // correctly handles default methods.
    if (methodDescriptorssBySignature == null) {
      methodDescriptorssBySignature = new LinkedHashMap<>();

      // Add all methods declared in the current type itself
      methodDescriptorssBySignature.putAll(getDeclaredMethodDescriptorsBySignature());

      // Add all the methods from the super class.
      if (getSuperTypeDescriptor() != null) {
        updateMethodsBySignature(
            methodDescriptorssBySignature, getSuperTypeDescriptor().getMethodDescriptors());
      }

      // Finally add the methods that appear in super interfaces.
      for (TypeDescriptor implementedInterface : getInterfaceTypeDescriptors()) {
        updateMethodsBySignature(
            methodDescriptorssBySignature, implementedInterface.getMethodDescriptors());
      }
    }
    return methodDescriptorssBySignature;
  }

  private static void updateMethodsBySignature(
      Map<String, MethodDescriptor> methodsBySignature,
      Iterable<MethodDescriptor> methodDescriptors) {
    for (MethodDescriptor declaredMethod : methodDescriptors) {
      MethodDescriptor existingMethod = methodsBySignature.get(declaredMethod.getMethodSignature());
      // TODO(rluble) implement correct default replacement when existing method != null.
      // Only replace the method if we found a default definition that implements the method at
      // that type; be sure to have all relevant examples, the semantics are quite particular.
      if (existingMethod == null) {
        methodsBySignature.put(declaredMethod.getMethodSignature(), declaredMethod);
      }
    }
  }

  /**
   * The list of methods declared in the type from the JDT. Note: this does not include methods we
   * synthesize and add to the type like bridge methods.
   */
  public Iterable<MethodDescriptor> getDeclaredMethodDescriptors() {
    return getDeclaredMethodDescriptorsBySignature().values();
  }

  /**
   * Retrieves the method descriptor with signature {@code signature} if there is a declared method
   * with that signature.
   */
  public MethodDescriptor getDeclaredMethodDescriptorBySignature(String signature) {
    return getDeclaredMethodDescriptorsBySignature().get(signature);
  }

  /**
   * Retrieves the method descriptor with name {@code name} and the corresponding parameter types if
   * there is a declared method with that signature.
   */
  public MethodDescriptor getDeclaredMethodDescriptorByName(
      String methodName, TypeDescriptor... parameters) {
    return getDeclaredMethodDescriptorsBySignature()
        .get(MethodDescriptors.getSignature(methodName, parameters));
  }

  /**
   * Retrieves the method descriptor with name {@code name} and the corresponding parameter types if
   * there is a method with that signature.
   */
  public MethodDescriptor getMethodDescriptorByName(
      String methodName, TypeDescriptor... parameters) {
    return getMethodDescriptorsBySignature()
        .get(MethodDescriptors.getSignature(methodName, parameters));
  }

  /** The list of all methods available on a given type. */
  public Iterable<MethodDescriptor> getMethodDescriptors() {
    return getMethodDescriptorsBySignature().values();
  }

  @Override
  public String toString() {
    return getUniqueId();
  }

  /** Returns a description that is useful for error messages. */
  @Override
  public String getReadableDescription() {
    // TODO: Actually provide a real readable description.
    return getSimpleName();
  }
}
