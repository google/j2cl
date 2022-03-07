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
 * Maps Java type members to Kotlin ones with different names.
 *
 * <p>This can only be applied to methods, fields (including {@code KtProperty} fields) and {@code
 * KtFactory} constructors in {@code KtType}-annotated classes/interfaces.
 *
 * <p>"Renaming" a constructor is implemented by replacing constructor invocations with invocations
 * of the companion method with the specified name. Such constructors cannot be used with {@code
 * super}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Documented
public @interface KtName {

  /** The Kotlin name of the member. */
  String value();
}
