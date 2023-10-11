/*
 * Copyright 2022 Google Inc.
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
package com.google.j2cl.transpiler.backend.kotlin.common

/**
 * Returns comparator which compares iterables using lexical order, where elements are compared
 * using this comparator.
 */
fun <T> Comparator<T>.lexical() =
  Comparator<Iterable<T>> { firstIterable, secondIterable ->
    compareLexical(firstIterable.iterator(), secondIterable.iterator())
  }

private tailrec fun <T> Comparator<T>.compareLexical(
  firstIterator: Iterator<T>,
  secondIterator: Iterator<T>
): Int =
  if (firstIterator.hasNext()) {
    if (secondIterator.hasNext()) {
      val result = compare(firstIterator.next(), secondIterator.next())
      if (result == 0) {
        compareLexical(firstIterator, secondIterator)
      } else {
        result
      }
    } else {
      1
    }
  } else {
    if (secondIterator.hasNext()) {
      -1
    } else {
      0
    }
  }
