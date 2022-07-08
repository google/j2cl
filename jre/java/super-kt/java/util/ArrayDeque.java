/*
 * Copyright 2016 Google Inc.
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

import javaemul.internal.annotations.KtName;
import javaemul.internal.annotations.KtNative;
import jsinterop.annotations.JsNonNull;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/ArrayDeque.html">the official
 * Java API doc</a> for details.
 */
@KtNative("kotlin.collections.ArrayDeque")
public class ArrayDeque<E> extends AbstractList<E> {

  public ArrayDeque() {}

  public ArrayDeque(int numElements) {}

  public ArrayDeque(Collection<? extends E> c) {}

  @Override
  public native boolean add(@JsNonNull E e);

  @Override
  public native void add(int index, @JsNonNull E element);

  public native void addFirst(@JsNonNull E e);

  public native void addLast(@JsNonNull E e);

  @Override
  public native void clear();

  @Override
  public native boolean contains(@JsNonNull E e);

  @KtName("first")
  public native @JsNonNull E element();

  @Override
  public native @JsNonNull E get(int index);

  @KtName("first")
  public native @JsNonNull E getFirst();

  @KtName("last")
  public native @JsNonNull E getLast();

  @Override
  public native boolean isEmpty();

  @Override
  public native @JsNonNull Iterator<E> iterator();

  @KtName("firstOrNull")
  public native E peek();

  @KtName("firstOrNull")
  public native E peekFirst();

  @KtName("lastOrNull")
  public native E peekLast();

  @KtName("removeFirstOrNull")
  public native E poll();

  @KtName("removeFirstOrNull")
  public native E pollFirst();

  @KtName("removeLastOrNull")
  public native E pollLast();

  @KtName("removeFirst")
  public native E pop();

  @KtName("addFirst")
  public native void push(@JsNonNull E e);

  @KtName("removeFirst")
  public native @JsNonNull E remove();

  @Override
  @KtName("removeAt")
  public native @JsNonNull E remove(int index);

  public native boolean remove(@JsNonNull E e);

  public native @JsNonNull E removeFirst();

  public native @JsNonNull E removeLast();

  @Override
  public native @JsNonNull E set(int index, @JsNonNull E element);

  @Override
  public native int size();
}
