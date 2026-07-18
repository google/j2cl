/*
 * Copyright 2025 Google Inc.
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
package j2kt;

import javaemul.internal.annotations.KtDisabled;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class AnnotationDefaults {

  enum SomeEnum {
    ZERO,
    ONE
  }

  @interface Bar {
    boolean booleanValue() default true;

    byte byteValue() default 1;

    char charValue() default 'a';

    short shortValue() default 2;

    int intValue() default 42;

    long longValue() default 3L;

    float floatValue() default 3.14f;

    double doubleValue() default 3.14159;

    int negativeValue() default -1;

    int calcValue() default 10 + 20 * 2;

    String stringValue() default "default";

    String escapedStringValue() default "hello\nworld";

    SomeEnum enumValue() default SomeEnum.ZERO;

    int[] intArray() default {1, 2, 3};

    boolean[] booleanArray() default {true, false};

    int[] emptyArray() default {};

    int[] shorthandArray() default 42;
  }

  @interface Positional {
    String value() default "positional";

    int count() default 1;
  }

  /** Tests accessing primitive default values of annotation features. */
  static void testDefaults_primitives(Bar bar) {
    boolean booleanValue = bar.booleanValue();
    byte byteValue = bar.byteValue();
    char charValue = bar.charValue();
    short shortValue = bar.shortValue();
    int intValue = bar.intValue();
    long longValue = bar.longValue();
    float floatValue = bar.floatValue();
    double doubleValue = bar.doubleValue();
    int negativeValue = bar.negativeValue();
    int calcValue = bar.calcValue();
  }

  /** Tests accessing Object default values of annotation features. */
  static void testDefaults_objects(Bar bar) {
    String stringValue = bar.stringValue();
    String escapedStringValue = bar.escapedStringValue();
    SomeEnum enumValue = bar.enumValue();
  }

  /** Tests accessing array default values of annotation features. */
  static void testDefaults_arrays(Bar bar) {
    int[] intArray = bar.intArray();
    boolean[] booleanArray = bar.booleanArray();
    int[] emptyArray = bar.emptyArray();
    int[] shorthandArray = bar.shorthandArray();
  }

  @interface WithTypeLiteral {
    Class<?> clazz() default String.class;

    Class<?>[] classes() default {Integer.class, Double.class};

    Class<?>[] shorthandClassArray() default Long.class;

    Class<?> genericClass() default java.util.List.class;

    Class<?> arrayClass() default String[].class;
  }

  // TODO(b/377373351): Convert values from kotlin.reflect.KClass to java.lang.Class
  @KtDisabled
  /** Tests accessing Class default values of annotation features. */
  static void testDefaults_classes(WithTypeLiteral withTypeLiteral) {
    Class<?> clazz = withTypeLiteral.clazz();
    Class<?>[] classes = withTypeLiteral.classes();
    Class<?>[] shorthandClassArray = withTypeLiteral.shorthandClassArray();
    Class<?> genericClass = withTypeLiteral.genericClass();
    Class<?> arrayClass = withTypeLiteral.arrayClass();
  }

  /** Tests accessing positional default values of annotation features. */
  static void testDefaults_positional(Positional positional) {
    String positionalValue = positional.value();
    int positionalCount = positional.count();
  }

  @Bar
  @Positional("custom")
  static class AnnotatedClass {}

  @interface Nested {
    int value() default 1;
  }

  // TODO(b/465873786): Support nested annotation default values.
  @interface WithNested {
    Nested nested() default @Nested(42);
  }

  // TODO(b/465873786): Enable once nested annotation default values are supported.
  @KtDisabled
  static void testDefaults_nested(WithNested withNested) {
    Nested nested = withNested.nested();
  }
}
