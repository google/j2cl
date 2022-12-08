/*
 * Copyright 2022 Google Inc.
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
package javaemul.internal.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kotlin {@code in} variance declaration.
 *
 * <p>Type parameters annotated with this annotation are rendered with {@code in} variance
 * declaration in Kotlin. When used in {@code KtNative} types, it provides variance information
 * about the corresponding native Kotlin type parameter.
 *
 * <p>The transpiler will also remove redundant {@code in} modifier from wildcards corresponding to
 * the annotated type parameter, so {@code Comparable<in T>} will be converted to {@code
 * Comparable<T>}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_PARAMETER)
@Documented
public @interface KtIn {}
