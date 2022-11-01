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

fun <T> Comparator<T>.ofList() = Comparator<List<T>> { a, b -> compareLists(a, b) }

private tailrec fun <T> Comparator<T>.compareLists(
  firstList: List<T>,
  secondList: List<T>,
  startIndex: Int = 0
): Int =
  if (startIndex >= firstList.size) if (startIndex >= secondList.size) 0 else -1
  else if (startIndex >= secondList.size) 1
  else {
    val itemCompare = compare(firstList[startIndex], secondList[startIndex])
    if (itemCompare != 0) itemCompare else compareLists(firstList, secondList, startIndex.inc())
  }
