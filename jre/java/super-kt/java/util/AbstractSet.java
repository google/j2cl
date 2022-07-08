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
package java.util;

import javaemul.internal.annotations.KtNative;
import jsinterop.annotations.JsNonNull;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/AbstractSet.html">the official
 * Java API doc</a> for details.
 */
@KtNative("kotlin.collections.AbstractMutableSet")
public abstract class AbstractSet<E> implements Set<E> {

  protected AbstractSet() {}

  @Override
  public abstract boolean add(@JsNonNull E o);

  @Override
  public native boolean addAll(@JsNonNull Collection<E> c);

  @Override
  public native void clear();

  @Override
  public native boolean contains(@JsNonNull E e);

  @Override
  public native boolean containsAll(@JsNonNull Collection<E> c);

  @Override
  public native boolean isEmpty();

  @Override
  public abstract @JsNonNull Iterator<E> iterator();

  @Override
  public native boolean remove(@JsNonNull E e);

  @Override
  public native boolean removeAll(@JsNonNull Collection<E> c);

  @Override
  public native boolean retainAll(@JsNonNull Collection<E> c);
}
