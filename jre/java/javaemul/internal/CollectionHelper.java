/*
 * Copyright 2023 Google Inc.
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
package javaemul.internal;

import java.util.Collection;
import java.util.Iterator;

public final class CollectionHelper {
  public static Object[] toArray(Collection<?> collection) {
    return toArray(collection, new Object[collection.size()]);
  }

  public static <E, T> T[] toArray(Collection<E> collection, T[] a) {
    int size = collection.size();
    if (a.length < size) {
      a = ArrayHelper.createFrom(a, size);
    }
    Object[] result = a;
    Iterator<E> it = collection.iterator();
    for (int i = 0; i < size; ++i) {
      result[i] = it.next();
    }
    if (a.length > size) {
      a[size] = null;
    }
    return a;
  }

  private CollectionHelper() {}
}
