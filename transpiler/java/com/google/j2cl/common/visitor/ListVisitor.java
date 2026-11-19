/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.common.visitor;

import java.util.List;
import java.util.function.BiFunction;

/** Visitor helper for lists. */
public class ListVisitor {
  /**
   * Visits the elements of {@code nodeList} with {@code acceptor}, replacing the elements for which
   * it returns a different node and removing the ones for which it returns {@code null}.
   *
   * <p>The processor is passed to the acceptor explicitly, rather than being captured by it, so
   * that callers can use a non-capturing lambda such as {@code (p, n) -> n.acceptInternal(p)}. The
   * JVM creates a single instance of a non-capturing lambda and reuses it for all evaluations of
   * the lambda expression, whereas a capturing lambda allocates a new instance each time, i.e. once
   * for every list that is visited.
   */
  public static <P, T> void visit(List<T> nodeList, P processor, BiFunction<P, T, T> acceptor) {
    for (int i = 0; i < nodeList.size(); i++) {
      T oldNode = nodeList.get(i);
      T newNode = acceptor.apply(processor, oldNode);
      if (newNode == null) {
        // Node is removed from list.
        nodeList.remove(i);
        i--;
        continue;
      }
      if (newNode != oldNode) {
        // Node is replaced.
        nodeList.set(i, newNode);
      }
    }
  }

  private ListVisitor() {}
}
