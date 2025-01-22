/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package annotation;

import java.lang.annotation.Annotation;
import javaemul.internal.annotations.KtDisabled;

public class Main {
  @interface Foo {
    boolean BOOLEAN_CONSTANT = false;

    int INT_CONSTANT = 123;

    String STRING_CONSTANT = "foo";

    boolean booleanValue() default BOOLEAN_CONSTANT;

    int intValue() default INT_CONSTANT;

    String stringValue() default STRING_CONSTANT;

    Class<?> classValue();

    Class<String> stringClassValue();

    Class<? extends Class<?>> wildcardClassValue();

    SomeEnum enumValue();

    Zoo annotationValue();

    boolean[] booleanArray();

    int[] intArray();

    String[] stringArray();

    Class<?>[] classArray();

    SomeEnum[] enumArray();

    Zoo[] annotationArray();
  }

  enum SomeEnum {
    ZERO,
    ONE
  }

  // Note that this inherits the single abstract method defined in its implicit parent
  // java.lang.annotation.Annotation. If this interface was not an annotation interface, it would
  // meet the requirements to be a functional interface.
  @interface Zoo {}

  static void test(Foo foo) {
    boolean booleanConstant = Foo.BOOLEAN_CONSTANT;
    int intConstant = Foo.INT_CONSTANT;
    String stringConstant = Foo.STRING_CONSTANT;

    boolean booleanValue = foo.booleanValue();
    int intValue = foo.intValue();
    String stringValue = foo.stringValue();
    SomeEnum enumValue = foo.enumValue();
    Zoo annotationValue = foo.annotationValue();

    boolean[] booleanArray = foo.booleanArray();
    int[] intArray = foo.intArray();
    String[] stringArray = foo.stringArray();
    SomeEnum[] enumArray = foo.enumArray();
    Zoo[] annotationArray = foo.annotationArray();
  }

  // TODO(b/377373351): Convert values from kotlin.reflect.KClass to java.lang.Class
  @KtDisabled
  static void testClassValues(Foo foo) {
    Class<?> annotatationType = foo.annotationType();
    Class<?> classValue = foo.classValue();
    Class<String> stringClassValue = foo.stringClassValue();
    Class<? extends Class<?>> wildcardClassValue = foo.wildcardClassValue();
    Class<?>[] classArray = foo.classArray();
  }

  abstract class ClassImplementingAnnotation implements Annotation {}

  static Annotation test(ClassImplementingAnnotation classImplementingAnnotation) {
    return classImplementingAnnotation;
  }

  interface InterfaceExtendingAnnotation extends Annotation {}

  static Annotation test(InterfaceExtendingAnnotation interfaceExtendingAnnotation) {
    return interfaceExtendingAnnotation;
  }
}
