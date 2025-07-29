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
package kotlinjavainterop;

import java.util.Collection;
import java.util.Iterator;

public class JavaIntSetExtendingIntermediateKotlinIntSet extends IntermediateKotlinIntSet {

  @Override
  public boolean add(Integer element) {
    return super.add(element);
  }

  @Override
  public boolean addAll(Collection<? extends Integer> elements) {
    return super.addAll(elements);
  }

  @Override
  public void clear() {
    super.clear();
  }

  @Override
  public Iterator<Integer> iterator() {
    return super.iterator();
  }

  @Override
  public boolean remove(Integer o) {
    return super.remove(o);
  }

  @Override
  public boolean removeAll(Collection elements) {
    return super.removeAll(elements);
  }

  @Override
  public boolean retainAll(Collection elements) {
    return super.retainAll(elements);
  }

  @Override
  public boolean contains(Integer o) {
    return super.contains(o);
  }

  @Override
  public boolean containsAll(Collection elements) {
    return super.containsAll(elements);
  }

  @Override
  public boolean isEmpty() {
    return super.isEmpty();
  }

  @Override
  public Object[] toArray() {
    return super.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return super.toArray(a);
  }

  @Override
  public int getSize() {
    return super.getSize();
  }
}
