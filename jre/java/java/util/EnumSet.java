/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package java.util;

import static javaemul.internal.InternalPreconditions.checkArgument;

/**
 * J2CL compatible implementation of EnumSet. Notably, some methods are not included since
 * Enum#getDeclaringClass is not supported for code size reasons.
 */
public class EnumSet<E extends Enum<E>> extends AbstractSet<E> implements Cloneable {
  private TreeMap<E, Object> map = new TreeMap<E, Object>();

  EnumSet() {}

  @Override
  public boolean add(E e) {
    Object old = map.put(e, this);
    return (old == null);
  }

  @Override
  public boolean remove(Object o) {
    return map.remove(o) != null;
  }

  @Override
  public boolean contains(Object o) {
    return map.containsKey(o);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Iterator<E> iterator() {
    return map.keySet().iterator();
  }

  public static <E extends Enum<E>> EnumSet<E> of(E first) {
    EnumSet<E> enumSet = new EnumSet();
    enumSet.add(first);
    return enumSet;
  }

  public static <E extends Enum<E>> EnumSet<E> of(E first, E... rest) {
    EnumSet<E> enumSet = new EnumSet();
    enumSet.add(first);
    for (E e : rest) {
      enumSet.add(e);
    }
    return enumSet;
  }

  public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> c) {
    checkArgument(c instanceof EnumSet || !c.isEmpty(), "Collection is empty");
    EnumSet<E> enumSet = new EnumSet();
    for (E e : c) {
      enumSet.add(e);
    }
    return enumSet;
  }

  public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType) {
    return new EnumSet();
  }

  public EnumSet<E> clone() {
    return EnumSet.copyOf(this);
  }
}
