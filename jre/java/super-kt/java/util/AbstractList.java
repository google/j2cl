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
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/AbstractList.html">the official
 * Java API doc</a> for details.
 */
@KtNative("kotlin.collections.AbstractMutableList")
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {

  protected transient int modCount;

  protected AbstractList() {}

  @Override
  public native boolean add(@JsNonNull E obj);

  @Override
  public abstract void add(int index, @JsNonNull E element);

  @Override
  public native boolean addAll(int index, @JsNonNull Collection<E> c);

  @Override
  public native int indexOf(@JsNonNull E toFind);

  @Override
  public native @JsNonNull Iterator<E> iterator();

  @Override
  public native int lastIndexOf(@JsNonNull E toFind);

  @Override
  public native @JsNonNull ListIterator<E> listIterator();

  @Override
  public native @JsNonNull ListIterator<E> listIterator(int from);

  @Override
  public abstract E remove(int index);

  @Override
  public native boolean removeAll(@JsNonNull Collection<E> c);

  protected native void removeRange(int fromIndex, int endIndex);

  @Override
  public native boolean retainAll(@JsNonNull Collection<E> c);

  @Override
  public abstract @JsNonNull E set(int index, E o);

  @Override
  public native @JsNonNull List<E> subList(int fromIndex, int toIndex);
}
