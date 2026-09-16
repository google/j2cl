/*
 * Copyright 2007 Google Inc.
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
package java.lang.annotation;

/**
 * Indicates an attempt to access an element of an annotation that has changed since it was compiled
 * or serialized <a
 * href="https://docs.oracle.com/en/java/javase/27/docs/api/java.base/java/lang/annotation/AnnotationTypeMismatchException.html">[official
 * Java API docs]</a>.
 */
public class AnnotationTypeMismatchException extends RuntimeException {}
