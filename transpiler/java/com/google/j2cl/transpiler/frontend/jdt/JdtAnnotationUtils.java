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
package com.google.j2cl.transpiler.frontend.jdt;

import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.SUPPRESS_WARNINGS_ANNOTATION_NAME;
import static java.util.Arrays.stream;

import com.google.common.base.Predicate;
import com.google.common.base.VerifyException;
import com.google.j2cl.transpiler.frontend.common.Nullability;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.internal.compiler.impl.BooleanConstant;
import org.eclipse.jdt.internal.compiler.impl.IntConstant;
import org.eclipse.jdt.internal.compiler.impl.StringConstant;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.ElementValuePair;

/** Utility functions to process JDT annotations. */
public final class JdtAnnotationUtils {
  @Nullable
  static IAnnotationBinding findAnnotationBindingByName(IBinding binding, String name) {
    if (!shouldReadAnnotations(binding)) {
      return null;
    }
    return findAnnotationBindingByName(binding.getAnnotations(), name);
  }

  @Nullable
  static IAnnotationBinding findAnnotationBindingByName(
      IAnnotationBinding[] annotations, String name) {
    if (annotations == null) {
      return null;
    }
    for (IAnnotationBinding annotationBinding : annotations) {
      if (annotationBinding.getAnnotationType().getQualifiedName().equals(name)) {
        return annotationBinding;
      }
    }
    return null;
  }

  static String getStringAttribute(IAnnotationBinding annotationBinding, String attributeName) {
    return getAttribute(annotationBinding, attributeName, String.class);
  }

  static Object[] getArrayAttribute(IAnnotationBinding annotationBinding, String attributeName) {
    return getAttribute(annotationBinding, attributeName, Object[].class);
  }

  static boolean getBooleanAttribute(
      IAnnotationBinding annotationBinding, String attributeName, boolean defaultValue) {
    Boolean value = getAttribute(annotationBinding, attributeName, Boolean.class);
    return value == null ? defaultValue : value;
  }

  /** Returns true if the binding is annotated with {@code annotationSourceName}. */
  static boolean hasAnnotation(IBinding binding, String annotationSourceName) {
    return JdtAnnotationUtils.findAnnotationBindingByName(binding, annotationSourceName) != null;
  }

  @SuppressWarnings("unchecked")
  private static <T> T getAttribute(
      IAnnotationBinding annotationBinding, String attributeName, Class<T> attributeClass) {
    if (annotationBinding == null) {
      return null;
    }
    // Use the compiler internal representation to obtain attribute values because the dom
    // model (IAnnotationBinding) would only return the attribute values if the annotation class
    // was present in the compile.
    AnnotationBinding internalAnnotationBinding = getAnnotationBinding(annotationBinding);
    for (ElementValuePair elementValuePair : internalAnnotationBinding.getElementValuePairs()) {
      if (!attributeName.equals(String.valueOf(elementValuePair.getName()))) {
        continue;
      }
      // Since this is the compiler internal representation, it is very raw, need to handle all
      // the conversions ourselves.
      Object value = convertValue(elementValuePair.getValue());
      if (attributeClass.isArray() && !value.getClass().isArray()) {
        // This is a singleton in an array annotation like @SuppressWarning("some-warning").
        return (T) new Object[] {value};
      }
      return (T) value;
    }
    return null;
  }

  private static Object convertValue(Object value) {
    if (value instanceof StringConstant constant) {
      return constant.stringValue();
    } else if (value instanceof BooleanConstant constant) {
      return constant.booleanValue();
    } else if (value instanceof IntConstant constant) {
      return constant.intValue();
    } else if (value instanceof Object[] constant) {
      return Arrays.stream(constant).map(JdtAnnotationUtils::convertValue).toArray(Object[]::new);
    }
    throw new IllegalStateException("Unexpected annotation attribute value type.");
  }

  /**
   * Reflective access to {@link AnnotationBinding#binding}.
   *
   * <p>JDT dom classes like IAnnotationBinding would not provide annotation attributes if the
   * annotation class is not present in the compile (even when accessing a previously compiled class
   * file). For that reason we obtain here, using reflection, the private reference to the internal
   * representation, i.e. org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding to extract the
   * attribute values.
   */
  private static AnnotationBinding getAnnotationBinding(IAnnotationBinding annotationBinding) {
    try {
      // Access to the internal compiler class AnnotationBinding through the private field
      // binding in the dom class implementing IAnnotationBinding.
      Field bindingField = annotationBinding.getClass().getDeclaredField("binding");
      bindingField.setAccessible(true);
      return (AnnotationBinding) bindingField.get(annotationBinding);
    } catch (ReflectiveOperationException e) {
      throw new VerifyException(
          "Unexpectedly unable to access AnnotationBinding.binding via reflection", e);
    }
  }

  @Nullable
  public static IAnnotationBinding getAnnotationBinding(
      PackageDeclaration packageDeclaration, Predicate<IAnnotationBinding> whichAnnotation) {
    List<Annotation> packageAnnotations =
        JdtEnvironment.asTypedList(packageDeclaration.annotations());
    if (packageAnnotations == null) {
      return null;
    }

    Optional<IAnnotationBinding> annotationBinding =
        packageAnnotations.stream()
            .map(Annotation::resolveAnnotationBinding)
            .filter(whichAnnotation)
            .findFirst();

    return annotationBinding.orElse(null);
  }

  @Nullable
  public static IAnnotationBinding getAnnotationBinding(
      ITypeBinding binding, Predicate<IAnnotationBinding> whichAnnotation) {
    if (!shouldReadAnnotations(binding)) {
      return null;
    }
    return Arrays.stream(binding.getAnnotations()).filter(whichAnnotation).findFirst().orElse(null);
  }

  public static boolean isWarningSuppressed(IBinding binding, String warning) {
    IAnnotationBinding annotationBinding = getSuppressWarningsAnnotation(binding);
    if (annotationBinding == null) {
      return false;
    }

    Object[] suppressions = JdtAnnotationUtils.getArrayAttribute(annotationBinding, "value");
    return stream(suppressions).anyMatch(warning::equals);
  }

  public static IAnnotationBinding getSuppressWarningsAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, SUPPRESS_WARNINGS_ANNOTATION_NAME);
  }

  public static boolean hasNullMarkedAnnotation(PackageDeclaration packageDeclaration) {
    return getAnnotationBinding(
            packageDeclaration,
            (a) -> Nullability.isNullMarkedAnnotation(a.getAnnotationType().getQualifiedName()))
        != null;
  }

  public static boolean hasNullMarkedAnnotation(ITypeBinding typeBinding) {
    return getAnnotationBinding(
            typeBinding,
            (a) -> Nullability.isNullMarkedAnnotation(a.getAnnotationType().getQualifiedName()))
        != null;
  }

  static boolean shouldReadAnnotations(IBinding binding) {
    // TODO(b/399417397) Determine if we should handle annotations on all annotation types. Remove
    // this method if necessary.
    if (binding instanceof ITypeBinding typeBinding) {
      if (typeBinding.isAnnotation() && !typeBinding.isFromSource()) {
        // Not all annotations are present in all compilations; in particular, Kotlin annotations
        // that on JsInterop annotations are only present when compiling their sources. As a
        // workaround here, annotations on annotations are only populated when compiling their
        // sources.
        return false;
      }
    }
    return true;
  }

  private JdtAnnotationUtils() {}
}
