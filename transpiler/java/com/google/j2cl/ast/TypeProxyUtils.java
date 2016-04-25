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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.PackageInfoCache;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility functions to transpile JDT TypeBinding to J2cl TypeDescriptor.
 */
public class TypeProxyUtils {
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
   * @param typeBinding the type binding, used to create the type descriptor.
   * @param elementAnnotations the annotations on the element
   */
  public static TypeDescriptor createTypeDescriptorWithNullability(
      ITypeBinding typeBinding, IAnnotationBinding[] elementAnnotations,
      Nullability defaultNullabilityForCompilationUnit) {
    TypeDescriptor descriptor;
    if (typeBinding.isArray()) {
      TypeDescriptor leafTypeDescriptor = createTypeDescriptorWithNullability(
          typeBinding.getElementType(),
          new IAnnotationBinding[0],
          defaultNullabilityForCompilationUnit);
      descriptor = TypeDescriptors.getForArray(leafTypeDescriptor, typeBinding.getDimensions());
    } else if (typeBinding.isParameterizedType()) {
      List<TypeDescriptor> typeArgumentsDescriptors = new ArrayList<>();
      for (ITypeBinding typeArgumentBinding : typeBinding.getTypeArguments()) {
        typeArgumentsDescriptors.add(createTypeDescriptorWithNullability(
            typeArgumentBinding, new IAnnotationBinding[0], defaultNullabilityForCompilationUnit));
      }
      descriptor = createTypeDescriptor(typeBinding, typeArgumentsDescriptors);
    } else {
      descriptor = createTypeDescriptor(typeBinding);
    }

    if (isNullable(typeBinding, elementAnnotations, defaultNullabilityForCompilationUnit)
        || descriptor.isTypeVariable()) {
      return descriptor;
    }
    return TypeDescriptors.toNonNullable(descriptor);
  }

  /**
   * Returns whether the given type binding should be nullable, according to the annotations on it
   * and if nullability is enabled for the package containing the binding.
   */
  private static boolean isNullable(
      ITypeBinding typeBinding, IAnnotationBinding[] elementAnnotations,
      Nullability defaultNullabilityForCompilationUnit) {
    if (typeBinding.isPrimitive()) {
      return false;
    }
    if (typeBinding.getQualifiedName().equals("java.lang.Void")) {
      // Void is nullable by default.
      return true;
    }
    if (defaultNullabilityForCompilationUnit == Nullability.NULL) {
      return true;
    }
    Iterable<IAnnotationBinding> allAnnotations =
        Iterables.concat(
            Arrays.asList(elementAnnotations),
            Arrays.asList(typeBinding.getTypeAnnotations()),
            Arrays.asList(typeBinding.getAnnotations()));
    for (IAnnotationBinding annotation : allAnnotations) {
      if (annotation.getName().equals("Nullable") || annotation.getName().equals("NullableType")) {
        return true;
      }
      // TODO(simionato): Consider supporting NotNull as well.
    }
    return false;
  }

  /**
   * Gets the default nullability for the given package, by examining the package-info file.
   */
  public static Nullability getPackageDefaultNullability(IPackageBinding packageBinding) {
    boolean nullabilityEnabled =
        PackageInfoCache.get().isNullabilityEnabled(packageBinding.getName());
    return nullabilityEnabled ? Nullability.NOT_NULL : Nullability.NULL;
  }

  /**
   * Creates a TypeDescriptor from a JDT TypeBinding.
   */
  public static TypeDescriptor createTypeDescriptor(ITypeBinding typeBinding,
      List<TypeDescriptor> typeArgumentDescriptors) {
    if (typeBinding == null) {
      return null;
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

  static List<TypeDescriptor> getTypeArgumentDescriptors(ITypeBinding typeBinding) {
    List<TypeDescriptor> typeArgumentDescriptors = new ArrayList<>();
    if (typeBinding.isParameterizedType()) {
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

  public static Visibility getVisibility(int modifiers) {
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

  static boolean isInstanceMemberClass(ITypeBinding typeBinding) {
    return typeBinding.isMember() && !Modifier.isStatic(typeBinding.getModifiers());
  }

  static boolean isInstanceNestedClass(ITypeBinding typeBinding) {
    return typeBinding.isNested() && !Modifier.isStatic(typeBinding.getModifiers());
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
    if (constructors.isEmpty()) {
      // A JsType with default constructor is a JsConstructor class.
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
  static boolean subclassesJsConstructorClass(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return false;
    }
    return isJsConstructorClass(typeBinding)
        || subclassesJsConstructorClass(typeBinding.getSuperclass());
  }

  /**
   * Returns the MethodDescriptor for the SAM method in JsFunction interface.
   */
  static MethodDescriptor getJsFunctionMethodDescriptor(ITypeBinding typeBinding) {
    IMethodBinding samInJsFunctionInterface = getSAMInJsFunctionInterface(typeBinding);
    return samInJsFunctionInterface == null
        ? null
        : JdtMethodUtils.createMethodDescriptor(samInJsFunctionInterface.getMethodDeclaration());
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
      return JdtMethodUtils.createMethodDescriptor(samInJsFunctionInterface);
    }
    for (IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
      if (methodBinding.isSynthetic()) {
        // skip the synthetic method.
        continue;
      }
      if (methodBinding.overrides(samInJsFunctionInterface)) {
        return JdtMethodUtils.createMethodDescriptor(methodBinding);
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
