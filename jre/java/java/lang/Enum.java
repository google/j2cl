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

  private final String name;

  private final int ordinal;

  protected Enum(String name, int ordinal) {
    this.name = name;
    this.ordinal = ordinal;
  }

  public Class<E> getDeclaringClass() {
    return null;
  }

  public final String name() {
    return name != null ? name : "" + ordinal;
  }

  public final int ordinal() {
    return ordinal;
  }

  @Override
  public int compareTo(E other) {
    return this.ordinal - ((Enum) other).ordinal;
  }

  @JsIgnore
  public static native <T extends Enum<T>> T valueOf(Class<T> enumType, String name) /*-{
    return null;
  }-*/;
}
