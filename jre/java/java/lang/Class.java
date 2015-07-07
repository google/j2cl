/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package java.lang;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Class.html">the
 * official Java API doc</a> for details.
 */
public abstract class Class<T> implements java.io.Serializable {

  public abstract boolean desiredAssertionStatus();

  public abstract String getCanonicalName();

  public abstract Class<?> getComponentType();

  public abstract T[] getEnumConstants();

  public abstract String getName();

  public abstract String getSimpleName();

  public abstract Class<?> getSuperclass();

  public abstract boolean isArray();

  public abstract boolean isEnum();

  public abstract boolean isInterface();

  public abstract boolean isPrimitive();

  public abstract String toString();
}
