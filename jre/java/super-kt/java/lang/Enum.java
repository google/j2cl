/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.lang;

import javaemul.internal.annotations.KtNative;
import javaemul.internal.annotations.KtProperty;
import jsinterop.annotations.JsNonNull;

@KtNative("kotlin.Enum")
public abstract class Enum<E extends @JsNonNull Enum<E>> implements Comparable<E> {

  protected Enum(String name, int ordinal) {}

  @KtProperty
  public final native String name();

  @KtProperty
  public final native int ordinal();

  @Override
  public native String toString();

  @Override
  public final native boolean equals(Object other);

  @Override
  public final native int hashCode();

  protected final native Object clone() throws CloneNotSupportedException;

  public final native int compareTo(E o);

  public final native Class<E> getDeclaringClass();

  public static native <T extends @JsNonNull Enum<T>> T valueOf(Class<T> enumType, String name);
}
