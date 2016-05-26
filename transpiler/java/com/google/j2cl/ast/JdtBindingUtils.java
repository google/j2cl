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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.j2cl.common.PackageInfoCache;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Utility functions to transpile JDT TypeBinding to J2cl TypeDescriptor.
 */
public class JdtBindingUtils {
  /**
   * The nullability of a package, type, class, etc.
   */
  public enum Nullability {
    NULL,
    NOT_NULL
  }

  /**
   * Creates a TypeDescriptor from a JDT TypeBinding.
   */
  // TODO(simionato): Delete this method and make all the callers use
  // createTypeDescriptorWithNullability.
  public static TypeDescriptor createTypeDescriptor(ITypeBinding typeBinding) {
    return createTypeDescriptor(typeBinding, null);
  }

  /**
   * Creates a type descriptor for the given type binding, taking into account nullability.
   *
   * @param typeBinding the type binding, used to create the type descriptor.
   * @param elementAnnotations the annotations on the element
   */
  public static TypeDescriptor createTypeDescriptorWithNullability(
      ITypeBinding typeBinding,
      IAnnotationBinding[] elementAnnotations,
      Nullability defaultNullabilityForCompilationUnit) {
    if (typeBinding == null) {
      return null;
    }
    TypeDescriptor descriptor;
    if (typeBinding.isArray()) {
      TypeDescriptor leafTypeDescriptor =
          createTypeDescriptorWithNullability(
              typeBinding.getElementType(),
              new IAnnotationBinding[0],
              defaultNullabilityForCompilationUnit);
      descriptor = TypeDescriptors.getForArray(leafTypeDescriptor, typeBinding.getDimensions());
    } else if (typeBinding.isParameterizedType()) {
      List<TypeDescriptor> typeArgumentsDescriptors = new ArrayList<>();
      for (ITypeBinding typeArgumentBinding : typeBinding.getTypeArguments()) {
        typeArgumentsDescriptors.add(
            createTypeDescriptorWithNullability(
                typeArgumentBinding,
                new IAnnotationBinding[0],
                defaultNullabilityForCompilationUnit));
      }
      descriptor = createTypeDescriptor(typeBinding, typeArgumentsDescriptors);
    } else {
      descriptor = createTypeDescriptor(typeBinding);
    }

    if (isNullable(typeBinding, elementAnnotations, defaultNullabilityForCompilationUnit)) {
      return TypeDescriptors.toNullable(descriptor);
    }
    return TypeDescriptors.toNonNullable(descriptor);
  }

  /**
   * Returns whether the given type binding should be nullable, according to the annotations on it
   * and if nullability is enabled for the package containing the binding.
   */
  private static boolean isNullable(
      ITypeBinding typeBinding,
      IAnnotationBinding[] elementAnnotations,
      Nullability defaultNullabilityForCompilationUnit) {
    Preconditions.checkNotNull(defaultNullabilityForCompilationUnit);
    if (typeBinding.isPrimitive()) {
      return false;
    }
    if (typeBinding.getQualifiedName().equals("java.lang.Void")) {
      // Void is always nullable.
      return true;
    }
    if (defaultNullabilityForCompilationUnit == Nullability.NOT_NULL) {
      Iterable<IAnnotationBinding> allAnnotations =
          Iterables.concat(
              Arrays.asList(elementAnnotations),
              Arrays.asList(typeBinding.getTypeAnnotations()),
              Arrays.asList(typeBinding.getAnnotations()));
      for (IAnnotationBinding annotation : allAnnotations) {
        if (annotation.getName().equals("Nullable")
            || annotation.getName().equals("NullableType")) {
          return true;
        }
        // TODO(simionato): Consider supporting NotNull as well.
      }
      return false;
    }
    return !typeBinding.isTypeVariable();
  }

  /**
   * Gets the default nullability for the given type by examining the package-info file in the
   * type's package in the same class path entry.
   */
  public static Nullability getTypeDefaultNullability(ITypeBinding typeBinding) {
    ITypeBinding topLevelTypeBinding = toTopLevelTypeBinding(typeBinding);

    if (topLevelTypeBinding.isFromSource()) {
      // Let the PackageInfoCache know that this class is Source, otherwise it would have to rummage
      // around in the class path to figure it out and it might even come up with the wrong answer
      // for example if this class has also been globbed into some other library that is a
      // dependency of this one.
      PackageInfoCache.get().markAsSource(topLevelTypeBinding.getBinaryName());
    }

    // For a top level class the binary and source name are the same.
    String sourceName = topLevelTypeBinding.getBinaryName();

    boolean nullabilityEnabled = PackageInfoCache.get().isNullabilityEnabled(sourceName);
    return nullabilityEnabled ? Nullability.NOT_NULL : Nullability.NULL;
  }

  /**
   * Incase the given type binding is nested, return the outermost possible enclosing type binding.
   */
  public static ITypeBinding toTopLevelTypeBinding(ITypeBinding typeBinding) {
    ITypeBinding topLevelClass = typeBinding;
    while (topLevelClass.getDeclaringClass() != null) {
      topLevelClass = topLevelClass.getDeclaringClass();
    }
    return topLevelClass;
  }
  
  private static boolean isIntersectionType(ITypeBinding binding) {
    boolean isIntersectionType = !binding.isPrimitive()
        && !binding.isCapture()
        && !binding.isArray()
        && !binding.isTypeVariable()
        && !binding.isWildcardType()
        && binding.getPackage() == null;
    if (isIntersectionType) {
      checkArgument(
          (binding.getSuperclass() != null && binding.getInterfaces().length >= 1)
              || (binding.getSuperclass() == null && binding.getInterfaces().length >= 2));
    }
    return isIntersectionType;
  }

  /**
   * This is a hack to get intersection types working temporarily.  We simply take the first type
   * in the intersection and return it.
   * TODO: Find out how to support intersection types in the jscompiler type system and fix this.
   */
  private static ITypeBinding getTypeForIntersectionType(ITypeBinding binding) {
    checkArgument(isIntersectionType(binding));
    return binding.getInterfaces()[0];
  }

  /**
   * Creates a TypeDescriptor from a JDT TypeBinding.
   */
  private static TypeDescriptor createTypeDescriptor(
      ITypeBinding typeBinding, List<TypeDescriptor> typeArgumentDescriptors) {
    if (typeBinding == null) {
      return null;
    }
    if (isIntersectionType(typeBinding)) {
      ITypeBinding intersectionType = getTypeForIntersectionType(typeBinding);
      return createTypeDescriptor(intersectionType, getTypeArgumentTypeDescriptors(typeBinding));
    }
    if (typeBinding.isArray()) {
      TypeDescriptor leafTypeDescriptor = createTypeDescriptor(typeBinding.getElementType());
      return TypeDescriptors.getForArray(leafTypeDescriptor, typeBinding.getDimensions());
    }
    return TypeDescriptors.createForType(typeBinding, typeArgumentDescriptors);
  }

  static List<String> getPackageComponents(ITypeBinding typeBinding) {
    List<String> packageComponents = new ArrayList<>();
    if (typeBinding.getPackage() != null) {
      packageComponents = Arrays.asList(typeBinding.getPackage().getNameComponents());
    }
    return packageComponents;
  }

  static List<String> getClassComponents(ITypeBinding typeBinding) {
    List<String> classComponents = new LinkedList<>();
    if (typeBinding.isWildcardType() || typeBinding.isCapture()) {
      return Arrays.asList("?");
    }
    ITypeBinding currentType = typeBinding;
    while (currentType != null) {
      String simpleName;
      if (currentType.isLocal()) {
        // JDT binary name for local class is like package.components.EnclosingClass$1SimpleName
        // Extract the name after the last '$' as the class component here.
        String binaryName = currentType.getErasure().getBinaryName();
        simpleName = binaryName.substring(binaryName.lastIndexOf('$') + 1);
      } else if (currentType.isTypeVariable()) {
        if (currentType.getDeclaringClass() != null) {
          // If it is a class-level type variable, use the simple name (with prefix "C_") as the
          // current name component.
          simpleName = AstUtils.TYPE_VARIABLE_IN_TYPE_PREFIX + currentType.getName();
        } else {
          // If it is a method-level type variable, use the simple name (with prefix "M_") as the
          // current name component, and add declaringClass_declaringMethod as the next name
          // component, and set currentType to declaringClass for the next iteration.
          classComponents.add(0, AstUtils.TYPE_VARIABLE_IN_METHOD_PREFIX + currentType.getName());
          simpleName =
              currentType.getDeclaringMethod().getDeclaringClass().getName()
                  + "_"
                  + currentType.getDeclaringMethod().getName();
          currentType = currentType.getDeclaringMethod().getDeclaringClass();
        }
      } else {
        simpleName = currentType.getErasure().getName();
      }
      classComponents.add(0, simpleName);
      currentType = currentType.getDeclaringClass();
    }
    return classComponents;
  }

  public static List<TypeDescriptor> getTypeArgumentTypeDescriptors(ITypeBinding typeBinding) {
    List<TypeDescriptor> typeArgumentDescriptors = new ArrayList<>();
    if (isIntersectionType(typeBinding)) {
      ITypeBinding intersectionTypeBinding = getTypeForIntersectionType(typeBinding);
      return getTypeArgumentTypeDescriptors(intersectionTypeBinding);
    } else if (typeBinding.isParameterizedType()) {
      typeArgumentDescriptors.addAll(createTypeDescriptors(typeBinding.getTypeArguments()));
    } else {
      typeArgumentDescriptors.addAll(createTypeDescriptors(typeBinding.getTypeParameters()));
      boolean isInstanceNestedClass =
          typeBinding.isNested() && !Modifier.isStatic(typeBinding.getModifiers());
      if (isInstanceNestedClass) {
        if (typeBinding.getDeclaringMethod() != null) {
          typeArgumentDescriptors.addAll(
              createTypeDescriptors(typeBinding.getDeclaringMethod().getTypeParameters()));
        }
        if (typeBinding.getDeclaringMember() == null
            || !Modifier.isStatic(typeBinding.getDeclaringMember().getModifiers())) {
          typeArgumentDescriptors.addAll(
              createTypeDescriptor(typeBinding.getDeclaringClass()).getTypeArgumentDescriptors());
        }
      }
    }
    return typeArgumentDescriptors;
  }

  public static Visibility getVisibility(IBinding binding) {
    return getVisibility(binding.getModifiers());
  }

  private static Visibility getVisibility(int modifiers) {
    if (Modifier.isPublic(modifiers)) {
      return Visibility.PUBLIC;
    } else if (Modifier.isProtected(modifiers)) {
      return Visibility.PROTECTED;
    } else if (Modifier.isPrivate(modifiers)) {
      return Visibility.PRIVATE;
    } else {
      return Visibility.PACKAGE_PRIVATE;
    }
  }

  public static boolean isDefaultMethod(IMethodBinding binding) {
    return Modifier.isDefault(binding.getModifiers());
  }

  public static boolean isAbstract(IBinding binding) {
    return Modifier.isAbstract(binding.getModifiers());
  }

  public static boolean isFinal(IBinding binding) {
    return Modifier.isFinal(binding.getModifiers());
  }

  public static boolean isStatic(IBinding binding) {
    return Modifier.isStatic(binding.getModifiers());
  }

  static boolean isInstanceMemberClass(ITypeBinding typeBinding) {
    return typeBinding.isMember() && !Modifier.isStatic(typeBinding.getModifiers());
  }

  static boolean isInstanceNestedClass(ITypeBinding typeBinding) {
    return typeBinding.isNested() && !Modifier.isStatic(typeBinding.getModifiers());
  }

  /**
   * Creates a MethodDescriptor directly based on the given JDT method binding.
   */
  public static MethodDescriptor createMethodDescriptor(IMethodBinding methodBinding) {
    int modifiers = methodBinding.getModifiers();
    boolean isStatic = Modifier.isStatic(modifiers);
    Visibility visibility = getVisibility(methodBinding);
    boolean isNative = Modifier.isNative(modifiers);
    TypeDescriptor enclosingClassTypeDescriptor =
        createTypeDescriptor(methodBinding.getDeclaringClass());
    boolean isConstructor = methodBinding.isConstructor();
    String methodName =
        isConstructor
            ? createTypeDescriptor(methodBinding.getDeclaringClass()).getBinaryClassName()
            : methodBinding.getName();
    final Nullability defaultNullabilityForPackage =
        getTypeDefaultNullability(methodBinding.getDeclaringClass());

    JsInfo jsInfo = computeJsInfo(methodBinding);

    TypeDescriptor returnTypeDescriptor =
        createTypeDescriptorWithNullability(
            methodBinding.getReturnType(),
            methodBinding.getAnnotations(),
            defaultNullabilityForPackage);

    // generate parameters type descriptors.
    List<TypeDescriptor> parameterTypeDescriptors = new ArrayList<>();
    for (int i = 0; i < methodBinding.getParameterTypes().length; i++) {
      TypeDescriptor descriptor =
          createTypeDescriptorWithNullability(
              methodBinding.getParameterTypes()[i],
              methodBinding.getParameterAnnotations(i),
              defaultNullabilityForPackage);
      parameterTypeDescriptors.add(descriptor);
    }

    MethodDescriptor declarationMethodDescriptor = null;
    if (methodBinding.getMethodDeclaration() != methodBinding) {
      declarationMethodDescriptor = createMethodDescriptor(methodBinding.getMethodDeclaration());
    }

    // generate type parameters declared in the method.
    Iterable<TypeDescriptor> typeParameterTypeDescriptors =
        FluentIterable.from(methodBinding.getTypeParameters())
            .transform(
                new Function<ITypeBinding, TypeDescriptor>() {
                  @Override
                  public TypeDescriptor apply(ITypeBinding typeBinding) {
                    return createTypeDescriptor(typeBinding);
                  }
                });

    return MethodDescriptor.Builder.fromDefault()
        .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
        .setMethodName(methodName)
        .setDeclarationMethodDescriptor(declarationMethodDescriptor)
        .setReturnTypeDescriptor(returnTypeDescriptor)
        .setParameterTypeDescriptors(parameterTypeDescriptors)
        .setTypeParameterTypeDescriptors(typeParameterTypeDescriptors)
        .setJsInfo(jsInfo)
        .setVisibility(visibility)
        .setIsStatic(isStatic)
        .setIsConstructor(isConstructor)
        .setIsNative(isNative)
        .setIsDefault(Modifier.isDefault(methodBinding.getModifiers()))
        .setIsVarargs(methodBinding.isVarargs())
        .build();
  }

  public static boolean isOrOverridesJsMember(IMethodBinding methodBinding) {
    return JsInteropUtils.isJsMember(methodBinding)
        || !getOverriddenJsMembers(methodBinding).isEmpty();
  }

  /**
   * Checks overriding chain to compute JsInfo.
   */
  static JsInfo computeJsInfo(IMethodBinding methodBinding) {
    List<JsInfo> jsInfoList = new ArrayList<>();
    // Add the JsInfo of the method and all the overridden methods to the list.
    JsInfo jsInfo = JsInteropUtils.getJsInfo(methodBinding);
    if (!jsInfo.isNone()) {
      jsInfoList.add(jsInfo);
    }
    for (IMethodBinding overriddenMethod : getOverriddenMethods(methodBinding)) {
      JsInfo inheritedJsInfo = JsInteropUtils.getJsInfo(overriddenMethod);
      if (!inheritedJsInfo.isNone()) {
        jsInfoList.add(inheritedJsInfo);
      }
    }

    if (jsInfoList.isEmpty()) {
      return JsInfo.NONE;
    }

    // TODO: Do the same for JsProperty?
    if (jsInfoList.get(0).getJsMemberType() == JsMemberType.METHOD) {
      // Return the first JsInfo with a Js name specified.
      for (JsInfo jsInfoElement : jsInfoList) {
        if (jsInfoElement.getJsName() != null) {
          return jsInfoElement;
        }
      }
    }
    return jsInfoList.get(0);
  }

  public static Set<IMethodBinding> getOverriddenJsMembers(IMethodBinding methodBinding) {
    return Sets.filter(
        getOverriddenMethods(methodBinding),
        new Predicate<IMethodBinding>() {
          @Override
          public boolean apply(IMethodBinding overriddenMethod) {
            return JsInteropUtils.isJsMember(overriddenMethod);
          }
        });
  }

  /**
   * Returns the method signature, which identifies a method up to overriding.
   */
  public static String getMethodSignature(IMethodBinding methodBinding) {
    StringBuilder signatureBuilder = new StringBuilder("");

    Visibility methodVisibility = getVisibility(methodBinding);
    if (methodVisibility.isPackagePrivate()) {
      signatureBuilder.append(":pp:");
      signatureBuilder.append(methodBinding.getDeclaringClass().getPackage());
      signatureBuilder.append(":");
    } else if (methodVisibility.isPrivate()) {
      signatureBuilder.append(":p:");
      signatureBuilder.append(methodBinding.getDeclaringClass().getBinaryName());
      signatureBuilder.append(":");
    }

    signatureBuilder.append(methodBinding.getName());
    signatureBuilder.append("(");

    String separator = "";
    for (ITypeBinding parameterType : methodBinding.getParameterTypes()) {
      signatureBuilder.append(separator);
      signatureBuilder.append(parameterType.getErasure().getBinaryName());
      separator = ";";
    }
    signatureBuilder.append(")");
    return signatureBuilder.toString();
  }

  public static Set<IMethodBinding> getOverriddenMethods(IMethodBinding methodBinding) {
    Set<IMethodBinding> overriddenMethods = new HashSet<>();
    ITypeBinding enclosingClass = methodBinding.getDeclaringClass();
    ITypeBinding superClass = enclosingClass.getSuperclass();
    if (superClass != null) {
      overriddenMethods.addAll(getOverriddenMethodsInType(methodBinding, superClass));
    }
    for (ITypeBinding superInterface : enclosingClass.getInterfaces()) {
      overriddenMethods.addAll(getOverriddenMethodsInType(methodBinding, superInterface));
    }
    return overriddenMethods;
  }

  static Set<IMethodBinding> getOverriddenMethodsInType(
      IMethodBinding methodBinding, ITypeBinding typeBinding) {
    Set<IMethodBinding> overriddenMethods = new HashSet<>();
    for (IMethodBinding declaredMethod : typeBinding.getDeclaredMethods()) {
      if (methodBinding.overrides(declaredMethod) && !methodBinding.isConstructor()) {
        checkArgument(!Modifier.isStatic(methodBinding.getModifiers()));
        overriddenMethods.add(declaredMethod);
      }
    }
    // Recurse into immediate super class and interfaces for overridden method.
    if (typeBinding.getSuperclass() != null) {
      overriddenMethods.addAll(
          getOverriddenMethodsInType(methodBinding, typeBinding.getSuperclass()));
    }
    for (ITypeBinding interfaceBinding : typeBinding.getInterfaces()) {
      overriddenMethods.addAll(getOverriddenMethodsInType(methodBinding, interfaceBinding));
    }
    return overriddenMethods;
  }

  static boolean isLocal(ITypeBinding typeBinding) {
    return typeBinding.isLocal();
  }

  /**
   * Returns true if {@code typeBinding} is a class that implements a JsFunction interface.
   */
  static boolean isJsFunctionImplementation(ITypeBinding typeBinding) {
    if (typeBinding == null || !typeBinding.isClass()) {
      return false;
    }
    for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
      if (JsInteropUtils.isJsFunction(superInterface)) {
        return true;
      }
    }
    return false;
  }

  static boolean isEnumOrSubclass(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return false;
    }
    return typeBinding.isEnum() || isEnumOrSubclass(typeBinding.getSuperclass());
  }

  /**
   * Returns true if the given type has a JsConstructor.
   */
  static boolean isJsConstructorClass(ITypeBinding typeBinding) {
    if (typeBinding == null || !typeBinding.isClass()) {
      return false;
    }
    Collection<IMethodBinding> constructors =
        Collections2.filter(
            Arrays.asList(typeBinding.getDeclaredMethods()),
            new Predicate<IMethodBinding>() {
              @Override
              public boolean apply(IMethodBinding method) {
                return method.isConstructor();
              }
            });
    if (constructors.isEmpty()
        && Modifier.isPublic(typeBinding.getModifiers())
        && !typeBinding.isEnum()) {
      // A public JsType with default constructor is a JsConstructor class.
      return JsInteropUtils.isJsType(typeBinding);
    }
    return !Collections2.filter(
            constructors,
            new Predicate<IMethodBinding>() {
              @Override
              public boolean apply(IMethodBinding constructor) {
                return JsInteropUtils.isJsConstructor(constructor);
              }
            })
        .isEmpty();
  }

  /**
   * Returns true if the given type has a JsConstructor, or it is a successor of a class that has a
   * JsConstructor.
   */
  static boolean isOrSubclassesJsConstructorClass(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return false;
    }
    return isJsConstructorClass(typeBinding)
        || isOrSubclassesJsConstructorClass(typeBinding.getSuperclass());
  }

  /**
   * Returns the MethodDescriptor for the SAM method in JsFunction interface.
   */
  static MethodDescriptor getJsFunctionMethodDescriptor(ITypeBinding typeBinding) {
    IMethodBinding samInJsFunctionInterface = getSAMInJsFunctionInterface(typeBinding);
    return samInJsFunctionInterface == null
        ? null
        : createMethodDescriptor(samInJsFunctionInterface.getMethodDeclaration());
  }

  /**
   * Returns the MethodDescriptor for the concrete JsFunction method implementation.
   */
  static MethodDescriptor getConcreteJsFunctionMethodDescriptor(ITypeBinding typeBinding) {
    IMethodBinding samInJsFunctionInterface = getSAMInJsFunctionInterface(typeBinding);
    if (samInJsFunctionInterface == null) {
      return null;
    }
    if (typeBinding.isInterface()) {
      return createMethodDescriptor(samInJsFunctionInterface);
    }
    for (IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
      if (methodBinding.isSynthetic()) {
        // skip the synthetic method.
        continue;
      }
      if (methodBinding.overrides(samInJsFunctionInterface)) {
        return createMethodDescriptor(methodBinding);
      }
    }
    return null;
  }

  /**
   * Returns JsFunction method in JsFunction interface.
   */
  private static IMethodBinding getSAMInJsFunctionInterface(ITypeBinding typeBinding) {
    if (!isJsFunctionImplementation(typeBinding) && !JsInteropUtils.isJsFunction(typeBinding)) {
      return null;
    }
    ITypeBinding jsFunctionInterface =
        typeBinding.isInterface() ? typeBinding : typeBinding.getInterfaces()[0];
    Preconditions.checkArgument(
        jsFunctionInterface.getDeclaredMethods().length == 1,
        "Type %s should have one and only one method.",
        jsFunctionInterface.getName());
    return jsFunctionInterface.getDeclaredMethods()[0];
  }

  private static List<TypeDescriptor> createTypeDescriptors(ITypeBinding[] typeBindings) {
    List<TypeDescriptor> typeDescriptors = new ArrayList<>();
    for (ITypeBinding typeBinding : typeBindings) {
      typeDescriptors.add(createTypeDescriptor(typeBinding));
    }
    return typeDescriptors;
  }
}
