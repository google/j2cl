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

import javaemul.internal.annotations.KtName;
import javaemul.internal.annotations.KtNative;
import javaemul.internal.annotations.KtPropagateNullability;
import jsinterop.annotations.JsNonNull;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/List.html">the official Java API
 * doc</a> for details.
 */
@KtNative("kotlin.collections.MutableList")
public interface List<E> extends Collection<E> {

  @KtPropagateNullability
  void add(int index, @JsNonNull E element);

  @KtPropagateNullability
  boolean addAll(int index, @JsNonNull Collection<E> c);

  @KtPropagateNullability
  @JsNonNull
  E get(int index);

  @KtPropagateNullability
  int indexOf(@JsNonNull E e);

  @KtPropagateNullability
  int lastIndexOf(@JsNonNull E e);

  @KtPropagateNullability
  @JsNonNull
  ListIterator<E> listIterator();

  @KtPropagateNullability
  @JsNonNull
  ListIterator<E> listIterator(int from);

  @KtName("removeAt")
  @KtPropagateNullability
  @JsNonNull
  E remove(int index);

  @KtPropagateNullability
  @JsNonNull
  E set(int index, @JsNonNull E element);

  @KtPropagateNullability
  @JsNonNull
  List<E> subList(int fromIndex, int toIndex);
}
