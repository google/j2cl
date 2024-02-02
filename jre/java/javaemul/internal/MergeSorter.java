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

import java.util.Comparator;

final class MergeSorter {
  /**
   * Performs a merge sort on the specified portion of an object array.
   *
   * <p>Uses O(n) temporary space to perform the merge, but is stable.
   */
  public static <T> void sort(T[] x, int fromIndex, int toIndex, Comparator<? super T> comp) {
    T[] temp = (T[]) ArrayHelper.unsafeClone(x, fromIndex, toIndex);
    mergeSort(temp, x, fromIndex, toIndex, -fromIndex, comp);
  }

  /**
   * Recursive helper function for {@link #sort(T[], int, int, Comparator)}.
   *
   * @param temp temporary space, as large as the range of elements being sorted. On entry, temp
   *     should contain a copy of the sort range from array.
   * @param array array to sort
   * @param low lower bound of range to sort
   * @param high upper bound of range to sort
   * @param ofs offset to convert an array index into a temp index
   * @param comp comparison function
   */
  private static <T> void mergeSort(
      T[] temp, T[] array, int low, int high, int ofs, Comparator<? super T> comp) {
    int length = high - low;

    // insertion sort for small arrays
    // TODO(goktug): Re-evaluate the cut point if we can use System.arrayCopy later down for copy.
    if (length < 17) {
      insertionSort(array, low, high, comp);
      return;
    }

    // recursively sort both halves, using the array as temp space
    int tempLow = low + ofs;
    int tempHigh = high + ofs;
    int tempMid = tempLow + ((tempHigh - tempLow) >> 1);
    mergeSort(array, temp, tempLow, tempMid, -ofs, comp);
    mergeSort(array, temp, tempMid, tempHigh, -ofs, comp);

    // Skip merge if already in order - just copy from temp
    if (comp.compare(temp[tempMid - 1], temp[tempMid]) <= 0) {
      // TODO(jat): use System.arraycopy when that is implemented and more
      // efficient than this
      while (low < high) {
        array[low++] = temp[tempLow++];
      }
      return;
    }

    // merge sorted halves
    merge(temp, tempLow, tempMid, tempHigh, array, low, high, comp);
  }

  /**
   * Sort a small subsection of an array by insertion sort.
   *
   * @param array array to sort
   * @param low lower bound of range to sort
   * @param high upper bound of range to sort
   * @param comp comparator to use
   */
  private static <T> void insertionSort(T[] array, int low, int high, Comparator<? super T> comp) {
    for (int i = low + 1; i < high; i++) {
      T curr = array[i];
      int j = i;
      while (j > low) {
        T prev = array[j - 1];
        if (comp.compare(prev, curr) <= 0) {
          break;
        }
        array[j] = prev;
        j--;
      }
      array[j] = curr;
    }
  }

  /**
   * Merge the two sorted subarrays (srcLow,srcMid] and (srcMid,srcHigh] into dest.
   *
   * @param src source array for merge
   * @param srcLow lower bound of bottom sorted half
   * @param srcMid upper bound of bottom sorted half & lower bound of top sorted half
   * @param srcHigh upper bound of top sorted half
   * @param dest destination array for merge
   * @param destLow lower bound of destination
   * @param destHigh upper bound of destination
   * @param comp comparator to use
   */
  private static <T> void merge(
      T[] src,
      int srcLow,
      int srcMid,
      int srcHigh,
      T[] dest,
      int destLow,
      int destHigh,
      Comparator<T> comp) {
    // can't destroy srcMid because we need it as a bound on the lower half
    int topIdx = srcMid;
    while (destLow < destHigh) {
      if (topIdx >= srcHigh || (srcLow < srcMid && comp.compare(src[srcLow], src[topIdx]) <= 0)) {
        dest[destLow++] = src[srcLow++];
      } else {
        dest[destLow++] = src[topIdx++];
      }
    }
  }

  private MergeSorter() {}
}
