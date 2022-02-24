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
 * Instructs J2KT to replace all references to this Java type with references to a native Kotlin
 * type. To be combined with {@code KtName} whenever Java and Kotlin type names differ.
 *
 * <ul>
 *   <li>{@code native} methods are interpreted as being implemented in the aliased Kotlin type.
 *   <li>methods that are not {@code native} must be abstract.
 *   <li>fields are mapped to the fields of the Kotlin type. Fields should not have any initializer.
 *       As a corollary, fields cannot be marked final. Readonly Kotlin properties should be mapped
 *       as non-final Java fields. The Kotlin compiler will prevent mutations of the property.
 *   <li>constructor bodies must be empty.
 *   <li>default methods and static methods in annotated interfaces must have empty bodies
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface KtType {}
