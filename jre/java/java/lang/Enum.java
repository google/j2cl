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

import java.io.Serializable;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

/**
 * See <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Enum.html">the official Java API
 * doc</a> for details.
 */
@JsType
public abstract class Enum<E extends Enum<E>> implements Comparable<E>, Serializable {

  // TODO(b/74986525): make final when fixed.
  private String name;
  private int ordinal;

  protected Enum(String name, int ordinal) {
    // TODO(b/74986525): uncomment initialization when fixed.
    // this.name = name;
    // this.ordinal = ordinal;
  }

  @JsIgnore
  public Class<E> getDeclaringClass() {
    Class<?> clazz = getClass();
    Class<?> superClass = clazz.getSuperclass();
    return (Class<E>) (superClass == Enum.class ? clazz : superClass);
  }

  public final String name() {
    return name != null ? name : "" + ordinal;
  }

  public final int ordinal() {
    return ordinal;
  }

  @Override
  public final int compareTo(E other) {
    return this.ordinal - ((Enum) other).ordinal;
  }

  @Override
  public final boolean equals(Object other) {
    return this == other;
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @JsIgnore
  public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return name();
  }
}
