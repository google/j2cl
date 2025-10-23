/*
 * Copyright 2025 Google Inc.
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
package j2ktiosinterop;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class CollectionTypes {

  public static <T extends @Nullable Object> Iterator<T> getIterator() {
    return Collections.emptyIterator();
  }

  public static <T extends @Nullable Object> CustomIterator<T> getCustomIterator() {
    return new CustomIterator<>();
  }

  public static <T extends @Nullable Object> ListIterator<T> getListIterator() {
    return Collections.emptyListIterator();
  }

  public static <T extends @Nullable Object> CustomListIterator<T> getCustomListIterator() {
    return new CustomListIterator<>();
  }

  public static <T extends @Nullable Object> Iterable<T> getIterable() {
    return Collections.emptyList();
  }

  public static <T extends @Nullable Object> CustomIterable<T> getCustomIterable() {
    return new CustomIterable<>();
  }

  public static <T extends @Nullable Object> Collection<T> getCollection() {
    return Collections.emptyList();
  }

  public static <T extends @Nullable Object> AbstractCollection<T> getAbstractCollection() {
    return new CustomCollection<>();
  }

  public static <T extends @Nullable Object> CustomCollection<T> getCustomCollection() {
    return new CustomCollection<>();
  }

  public static <T extends @Nullable Object> List<T> getList() {
    return Collections.emptyList();
  }

  public static <T extends @Nullable Object> ArrayList<T> getArrayList() {
    return new ArrayList<>();
  }

  public static <T extends @Nullable Object> LinkedList<T> getLinkedList() {
    return new LinkedList<>();
  }

  public static <T extends @Nullable Object> AbstractList<T> getAbstractList() {
    return new CustomList<>();
  }

  public static <T extends @Nullable Object> CustomList<T> getCustomList() {
    return new CustomList<>();
  }

  public static <T extends @Nullable Object> Set<T> getSet() {
    return Collections.emptySet();
  }

  public static <T extends @Nullable Object> HashSet<T> getHashSet() {
    return new HashSet<>();
  }

  public static <T extends @Nullable Object> AbstractSet<T> getAbstractSet() {
    return new CustomSet<>();
  }

  public static <T extends @Nullable Object> CustomSet<T> getCustomSet() {
    return new CustomSet<>();
  }

  public static <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> getMap() {
    return Collections.emptyMap();
  }

  public static <K extends @Nullable Object, V extends @Nullable Object>
      HashMap<K, V> getHashMap() {
    return new HashMap<>();
  }

  public static <K extends @Nullable Object, V extends @Nullable Object>
      LinkedHashMap<K, V> getLinkedHashMap() {
    return new LinkedHashMap<>();
  }

  public static <K extends @Nullable Object, V extends @Nullable Object>
      AbstractMap<K, V> getAbstractMap() {
    return new CustomMap<>();
  }

  public static <K extends @Nullable Object, V extends @Nullable Object>
      CustomMap<K, V> getCustomMap() {
    return new CustomMap<>();
  }

  public static <T extends @Nullable Object> void acceptIterator(Iterator<T> iterator) {}

  public static <T extends @Nullable Object> void acceptCustomIterator(
      CustomIterator<T> iterator) {}

  public static <T extends @Nullable Object> void acceptListIterator(ListIterator<T> iterator) {}

  public static <T extends @Nullable Object> void acceptCustomListIterator(
      CustomListIterator<T> iterator) {}

  public static <T extends @Nullable Object> void acceptIterable(Iterable<T> iterable) {}

  public static <T extends @Nullable Object> void acceptCustomIterable(
      CustomIterable<T> iterable) {}

  public static <T extends @Nullable Object> void acceptCollection(Collection<T> collection) {}

  public static <T extends @Nullable Object> void acceptAbstractCollection(
      AbstractCollection<T> collection) {}

  public static <T extends @Nullable Object> void acceptCustomCollection(
      CustomCollection<T> collection) {}

  public static <T extends @Nullable Object> void acceptList(List<T> list) {}

  public static <T extends @Nullable Object> void acceptArrayList(ArrayList<T> list) {}

  public static <T extends @Nullable Object> void acceptLinkedList(LinkedList<T> list) {}

  public static <T extends @Nullable Object> void acceptAbstractList(AbstractList<T> list) {}

  public static <T extends @Nullable Object> void acceptCustomList(CustomList<T> list) {}

  public static <T extends @Nullable Object> void acceptSet(Set<T> set) {}

  public static <T extends @Nullable Object> void acceptHashSet(HashSet<T> set) {}

  public static <T extends @Nullable Object> void acceptAbstractSet(AbstractSet<T> set) {}

  public static <T extends @Nullable Object> void acceptCustomSet(CustomSet<T> set) {}

  public static <K extends @Nullable Object, V extends @Nullable Object> void acceptMap(
      Map<K, V> map) {}

  public static <K extends @Nullable Object, V extends @Nullable Object> void acceptHashMap(
      HashMap<K, V> map) {}

  public static <K extends @Nullable Object, V extends @Nullable Object> void acceptLinkedHashMap(
      LinkedHashMap<K, V> map) {}

  public static <K extends @Nullable Object, V extends @Nullable Object> void acceptAbstractMap(
      AbstractMap<K, V> map) {}

  public static <K extends @Nullable Object, V extends @Nullable Object> void acceptCustomMap(
      CustomMap<K, V> map) {}

  public static final class CustomIterator<T extends @Nullable Object> implements Iterator<T> {
    @Override
    public T next() {
      throw new NoSuchElementException();
    }

    @Override
    public boolean hasNext() {
      return false;
    }

    public static <T extends @Nullable Object> Builder<T> builder() {
      return new Builder<>();
    }

    public static final class Builder<T extends @Nullable Object> {
      public CustomIterator<T> build() {
        return new CustomIterator<>();
      }
    }
  }

  public static final class CustomListIterator<T extends @Nullable Object>
      implements ListIterator<T> {
    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int previousIndex() {
      return -1;
    }

    @Override
    public T previous() {
      throw new NoSuchElementException();
    }

    @Override
    public int nextIndex() {
      return 0;
    }

    @Override
    public T next() {
      throw new NoSuchElementException();
    }

    @Override
    public boolean hasPrevious() {
      return false;
    }

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public void add(T t) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void set(T t) {
      throw new UnsupportedOperationException();
    }

    public static <T extends @Nullable Object> Builder<T> builder() {
      return new Builder<>();
    }

    public static final class Builder<T extends @Nullable Object> {
      public CustomListIterator<T> build() {
        return new CustomListIterator<>();
      }
    }
  }

  public static final class CustomIterable<T extends @Nullable Object> implements Iterable<T> {
    @Override
    public Iterator<T> iterator() {
      return new CustomIterator<>();
    }

    public static <T extends @Nullable Object> Builder<T> builder() {
      return new Builder<>();
    }

    public static final class Builder<T extends @Nullable Object> {
      public CustomIterable<T> build() {
        return new CustomIterable<>();
      }
    }
  }

  public static final class CustomCollection<T extends @Nullable Object>
      extends AbstractCollection<T> {
    @Override
    public int size() {
      return 0;
    }

    @Override
    public Iterator<T> iterator() {
      return new CustomIterator<>();
    }

    public static <T extends @Nullable Object> Builder<T> builder() {
      return new Builder<>();
    }

    public static final class Builder<T extends @Nullable Object> {
      public CustomCollection<T> build() {
        return new CustomCollection<>();
      }
    }
  }

  public static final class CustomList<T extends @Nullable Object> extends AbstractList<T> {
    @Override
    public int size() {
      return 0;
    }

    @Override
    public T get(int index) {
      throw new IndexOutOfBoundsException();
    }

    public static <T extends @Nullable Object> Builder<T> builder() {
      return new Builder<>();
    }

    public static final class Builder<T extends @Nullable Object> {
      public CustomList<T> build() {
        return new CustomList<>();
      }
    }
  }

  public static final class CustomSet<T extends @Nullable Object> extends AbstractSet<T> {
    @Override
    public int size() {
      return 0;
    }

    @Override
    public Iterator<T> iterator() {
      return new CustomIterator<>();
    }

    public static <T extends @Nullable Object> Builder<T> builder() {
      return new Builder<>();
    }

    public static final class Builder<T extends @Nullable Object> {
      public CustomSet<T> build() {
        return new CustomSet<>();
      }
    }
  }

  public static final class CustomMap<K extends @Nullable Object, V extends @Nullable Object>
      extends AbstractMap<K, V> {
    @Override
    public Set<Entry<K, V>> entrySet() {
      return new CustomSet<>();
    }

    public static <K extends @Nullable Object, V extends @Nullable Object> Builder<K, V> builder() {
      return new Builder<>();
    }

    public static final class Builder<K extends @Nullable Object, V extends @Nullable Object> {
      public CustomMap<K, V> build() {
        return new CustomMap<>();
      }
    }
  }

  private CollectionTypes() {}
}
