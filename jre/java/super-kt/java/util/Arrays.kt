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

package java.util

import java.util.function.BinaryOperator
import java.util.function.DoubleBinaryOperator
import java.util.function.IntBinaryOperator
import java.util.function.IntFunction
import java.util.function.IntToDoubleFunction
import java.util.function.IntToLongFunction
import java.util.function.IntUnaryOperator
import java.util.function.LongBinaryOperator
import javaemul.internal.InternalPreconditions.Companion.checkCriticalArrayBounds

/**
 * Utility methods related to native arrays. See <a
 * href="https://docs.oracle.com/javase/8/docs/api/java/util/Arrays.html"> the official Java API
 * doc</a> for details.
 */
object Arrays {
  // TODO(b/239034072): Revisit set after varargs are fixed
  fun <T> asList(vararg elements: T): MutableList<T> {
    val array: Array<T> = elements as Array<T>
    requireNotNull(array)
    return object : AbstractMutableList<T>(), RandomAccess {
      override val size: Int
        get() = array.size
      override fun isEmpty(): Boolean = array.isEmpty()
      override fun contains(element: T): Boolean = array.contains(element)
      override fun get(index: Int): T = array[index]
      override fun indexOf(element: T): Int = this.indexOf(element)
      override fun lastIndexOf(element: T): Int = this.lastIndexOf(element)
      override fun set(index: Int, element: T): T {
        val prevValue = array[index]
        array[index] = element
        return prevValue
      }
      override fun add(index: Int, element: T) = throw UnsupportedOperationException()
      override fun removeAt(index: Int) = throw UnsupportedOperationException()
    }
  }

  fun binarySearch(sortedArray: ByteArray?, fromIndex: Int, toIndex: Int, key: Byte): Int {
    requireNotNull(sortedArray)
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.size)
    return sortedArray.asList().binarySearch(key, fromIndex, toIndex)
  }

  fun binarySearch(sortedArray: ByteArray?, key: Byte): Int {
    requireNotNull(sortedArray)
    return sortedArray.asList().binarySearch(key)
  }

  fun binarySearch(sortedArray: CharArray?, fromIndex: Int, toIndex: Int, key: Char): Int {
    requireNotNull(sortedArray)
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.size)
    return sortedArray.asList().binarySearch(key, fromIndex, toIndex)
  }

  fun binarySearch(sortedArray: CharArray?, key: Char): Int {
    requireNotNull(sortedArray)
    return sortedArray.asList().binarySearch(key)
  }

  fun binarySearch(sortedArray: DoubleArray?, fromIndex: Int, toIndex: Int, key: Double): Int {
    requireNotNull(sortedArray)
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.size)
    return sortedArray.asList().binarySearch(key, fromIndex, toIndex)
  }

  fun binarySearch(sortedArray: DoubleArray?, key: Double): Int {
    requireNotNull(sortedArray)
    return sortedArray.asList().binarySearch(key)
  }

  fun binarySearch(sortedArray: FloatArray?, fromIndex: Int, toIndex: Int, key: Float): Int {
    requireNotNull(sortedArray)
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.size)
    return sortedArray.asList().binarySearch(key, fromIndex, toIndex)
  }

  fun binarySearch(sortedArray: FloatArray?, key: Float): Int {
    requireNotNull(sortedArray)
    return sortedArray.asList().binarySearch(key)
  }

  fun binarySearch(sortedArray: IntArray?, fromIndex: Int, toIndex: Int, key: Int): Int {
    requireNotNull(sortedArray)
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.size)
    return sortedArray.asList().binarySearch(key, fromIndex, toIndex)
  }

  fun binarySearch(sortedArray: IntArray?, key: Int): Int {
    requireNotNull(sortedArray)
    return sortedArray.asList().binarySearch(key)
  }

  fun binarySearch(sortedArray: LongArray?, fromIndex: Int, toIndex: Int, key: Long): Int {
    requireNotNull(sortedArray)
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.size)
    return sortedArray.asList().binarySearch(key, fromIndex, toIndex)
  }

  fun binarySearch(sortedArray: LongArray?, key: Long): Int {
    requireNotNull(sortedArray)
    return sortedArray.asList().binarySearch(key)
  }

  fun binarySearch(sortedArray: Array<Any?>?, fromIndex: Int, toIndex: Int, key: Any?): Int {
    return binarySearch(sortedArray, fromIndex, toIndex, key, null)
  }

  fun binarySearch(sortedArray: Array<Any?>?, key: Any?): Int {
    return binarySearch(sortedArray, key, null)
  }

  fun binarySearch(sortedArray: ShortArray?, fromIndex: Int, toIndex: Int, key: Short): Int {
    requireNotNull(sortedArray)
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.size)
    return sortedArray.toList().binarySearch(key, fromIndex, toIndex)
  }

  fun binarySearch(sortedArray: ShortArray?, key: Short): Int {
    requireNotNull(sortedArray)
    return sortedArray.toList().binarySearch(key)
  }

  fun <T> binarySearch(
    sortedArray: Array<T>?,
    fromIndex: Int,
    toIndex: Int,
    key: T,
    comparator: Comparator<in T>?
  ): Int {
    requireNotNull(sortedArray)
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.size)
    val comparator = Comparators.nullToNaturalOrder(comparator)
    return sortedArray.asList().binarySearch(key, comparator, fromIndex, toIndex)
  }

  fun <T> binarySearch(sortedArray: Array<T>?, key: T, c: Comparator<in T>?): Int {
    requireNotNull(sortedArray)
    val c = Comparators.nullToNaturalOrder(c)
    return sortedArray.asList().binarySearch(key, c)
  }

  fun copyOf(original: BooleanArray?, newLength: Int): BooleanArray {
    requireNotNull(original)
    return original.copyOf(newLength)
  }

  fun copyOf(original: ByteArray?, newLength: Int): ByteArray {
    requireNotNull(original)
    return original.copyOf(newLength)
  }

  fun copyOf(original: CharArray?, newLength: Int): CharArray {
    requireNotNull(original)
    return original.copyOf(newLength)
  }

  fun copyOf(original: DoubleArray?, newLength: Int): DoubleArray {
    requireNotNull(original)
    return original.copyOf(newLength)
  }

  fun copyOf(original: FloatArray?, newLength: Int): FloatArray {
    requireNotNull(original)
    return original.copyOf(newLength)
  }

  fun copyOf(original: IntArray?, newLength: Int): IntArray {
    requireNotNull(original)
    return original.copyOf(newLength)
  }

  fun copyOf(original: LongArray?, newLength: Int): LongArray {
    requireNotNull(original)
    return original.copyOf(newLength)
  }

  fun copyOf(original: ShortArray?, newLength: Int): ShortArray {
    requireNotNull(original)
    return original.copyOf(newLength)
  }

  fun <T> copyOf(original: Array<T>?, newLength: Int): Array<T?> {
    requireNotNull(original)
    return original.copyOf(newLength)
  }

  fun copyOfRange(original: BooleanArray?, from: Int, to: Int): BooleanArray {
    requireNotNull(original)
    return original.copyOfRange(from, to)
  }

  fun copyOfRange(original: ByteArray?, from: Int, to: Int): ByteArray {
    requireNotNull(original)
    return original.copyOfRange(from, to)
  }

  fun copyOfRange(original: CharArray?, from: Int, to: Int): CharArray {
    requireNotNull(original)
    return original.copyOfRange(from, to)
  }

  fun copyOfRange(original: DoubleArray?, from: Int, to: Int): DoubleArray {
    requireNotNull(original)
    return original.copyOfRange(from, to)
  }

  fun copyOfRange(original: FloatArray?, from: Int, to: Int): FloatArray {
    requireNotNull(original)
    return original.copyOfRange(from, to)
  }

  fun copyOfRange(original: IntArray?, from: Int, to: Int): IntArray {
    requireNotNull(original)
    return original.copyOfRange(from, to)
  }

  fun copyOfRange(original: LongArray?, from: Int, to: Int): LongArray {
    requireNotNull(original)
    return original.copyOfRange(from, to)
  }

  fun copyOfRange(original: ShortArray?, from: Int, to: Int): ShortArray {
    requireNotNull(original)
    return original.copyOfRange(from, to)
  }

  fun <T> copyOfRange(original: Array<T>?, from: Int, to: Int): Array<T> {
    requireNotNull(original)
    return original.copyOfRange(from, to)
  }

  fun deepEquals(a1: Array<Any?>?, a2: Array<Any?>?): Boolean {
    return a1.contentDeepEquals(a2)
  }

  fun deepHashCode(a: Array<Any?>?): Int {
    return a.contentDeepHashCode()
  }

  fun deepToString(a: Array<Any?>?): String {
    return a.contentDeepToString()
  }

  fun equals(array1: BooleanArray?, array2: BooleanArray?): Boolean {
    return array1 contentEquals array2
  }

  fun equals(array1: ByteArray?, array2: ByteArray?): Boolean {
    return array1 contentEquals array2
  }

  fun equals(array1: CharArray?, array2: CharArray?): Boolean {
    return array1 contentEquals array2
  }

  fun equals(array1: DoubleArray?, array2: DoubleArray?): Boolean {
    return array1 contentEquals array2
  }

  fun equals(array1: FloatArray?, array2: FloatArray?): Boolean {
    return array1 contentEquals array2
  }

  fun equals(array1: IntArray?, array2: IntArray?): Boolean {
    return array1 contentEquals array2
  }

  fun equals(array1: LongArray?, array2: LongArray?): Boolean {
    return array1 contentEquals array2
  }

  fun equals(array1: Array<Any?>?, array2: Array<Any?>?): Boolean {
    return array1 contentEquals array2
  }

  fun equals(array1: ShortArray?, array2: ShortArray?): Boolean {
    return array1 contentEquals array2
  }

  fun fill(a: BooleanArray?, `val`: Boolean) {
    requireNotNull(a)
    a.fill(`val`)
  }

  fun fill(a: BooleanArray?, fromIndex: Int, toIndex: Int, `val`: Boolean) {
    requireNotNull(a)
    a.fill(`val`, fromIndex, toIndex)
  }

  fun fill(a: ByteArray?, `val`: Byte) {
    requireNotNull(a)
    a.fill(`val`)
  }

  fun fill(a: ByteArray?, fromIndex: Int, toIndex: Int, `val`: Byte) {
    requireNotNull(a)
    a.fill(`val`, fromIndex, toIndex)
  }

  fun fill(a: CharArray?, `val`: Char) {
    requireNotNull(a)
    a.fill(`val`)
  }

  fun fill(a: CharArray?, fromIndex: Int, toIndex: Int, `val`: Char) {
    requireNotNull(a)
    a.fill(`val`, fromIndex, toIndex)
  }

  fun fill(a: DoubleArray?, `val`: Double) {
    requireNotNull(a)
    a.fill(`val`)
  }

  fun fill(a: DoubleArray?, fromIndex: Int, toIndex: Int, `val`: Double) {
    requireNotNull(a)
    a.fill(`val`, fromIndex, toIndex)
  }

  fun fill(a: FloatArray?, `val`: Float) {
    requireNotNull(a)
    a.fill(`val`)
  }

  fun fill(a: FloatArray?, fromIndex: Int, toIndex: Int, `val`: Float) {
    requireNotNull(a)
    a.fill(`val`, fromIndex, toIndex)
  }

  fun fill(a: IntArray?, `val`: Int) {
    requireNotNull(a)
    a.fill(`val`)
  }

  fun fill(a: IntArray?, fromIndex: Int, toIndex: Int, `val`: Int) {
    requireNotNull(a)
    a.fill(`val`, fromIndex, toIndex)
  }

  fun fill(a: LongArray?, `val`: Long) {
    requireNotNull(a)
    a.fill(`val`)
  }

  fun fill(a: LongArray?, fromIndex: Int, toIndex: Int, `val`: Long) {
    requireNotNull(a)
    a.fill(`val`, fromIndex, toIndex)
  }

  fun fill(a: Array<Any?>?, `val`: Any?) {
    requireNotNull(a)
    a.fill(`val`)
  }

  fun fill(a: Array<Any?>?, fromIndex: Int, toIndex: Int, `val`: Any?) {
    requireNotNull(a)
    a.fill(`val`, fromIndex, toIndex)
  }

  fun fill(a: ShortArray?, `val`: Short) {
    requireNotNull(a)
    a.fill(`val`)
  }

  fun fill(a: ShortArray?, fromIndex: Int, toIndex: Int, `val`: Short) {
    requireNotNull(a)
    a.fill(`val`, fromIndex, toIndex)
  }

  fun hashCode(a: BooleanArray?): Int {
    return a.contentHashCode()
  }

  fun hashCode(a: ByteArray?): Int {
    return a.contentHashCode()
  }

  fun hashCode(a: CharArray?): Int {
    return a.contentHashCode()
  }

  fun hashCode(a: DoubleArray?): Int {
    return a.contentHashCode()
  }

  fun hashCode(a: FloatArray?): Int {
    return a.contentHashCode()
  }

  fun hashCode(a: IntArray?): Int {
    return a.contentHashCode()
  }

  fun hashCode(a: LongArray?): Int {
    return a.contentHashCode()
  }

  fun hashCode(a: Array<Any?>?): Int {
    return a.contentHashCode()
  }

  fun hashCode(a: ShortArray?): Int {
    return a.contentHashCode()
  }

  fun toString(a: Array<*>?): String {
    requireNotNull(a)
    return a.contentToString()
  }

  fun toString(a: BooleanArray?): String {
    requireNotNull(a)
    return a.contentToString()
  }

  fun toString(a: ByteArray?): String {
    requireNotNull(a)
    return a.contentToString()
  }

  fun toString(a: CharArray?): String {
    requireNotNull(a)
    return a.contentToString()
  }

  fun toString(a: DoubleArray?): String {
    requireNotNull(a)
    return a.contentToString()
  }

  fun toString(a: FloatArray?): String {
    requireNotNull(a)
    return a.contentToString()
  }

  fun toString(a: IntArray?): String {
    requireNotNull(a)
    return a.contentToString()
  }

  fun toString(a: LongArray?): String {
    requireNotNull(a)
    return a.contentToString()
  }

  fun toString(a: ShortArray?): String {
    requireNotNull(a)
    return a.contentToString()
  }

  // TODO(b/238392635): Parallelize parallelPrefix, currently a single-threaded operation
  fun parallelPrefix(array: DoubleArray?, op: DoubleBinaryOperator) {
    requireNotNull(array)
    parallelPrefix0(array, 0, array.size, op)
  }

  fun parallelPrefix(array: DoubleArray?, fromIndex: Int, toIndex: Int, op: DoubleBinaryOperator) {
    requireNotNull(array)
    checkCriticalArrayBounds(fromIndex, toIndex, array.size)
    parallelPrefix0(array, fromIndex, toIndex, op)
  }

  private fun parallelPrefix0(
    array: DoubleArray,
    fromIndex: Int,
    toIndex: Int,
    op: DoubleBinaryOperator
  ) {
    var acc: Double = array[fromIndex]
    for (i in fromIndex + 1 until toIndex) {
      acc = op.applyAsDouble(acc, array[i])
      array[i] = acc
    }
  }

  fun parallelPrefix(array: IntArray?, op: IntBinaryOperator) {
    requireNotNull(array)
    parallelPrefix0(array, 0, array.size, op)
  }

  fun parallelPrefix(array: IntArray?, fromIndex: Int, toIndex: Int, op: IntBinaryOperator) {
    requireNotNull(array)
    checkCriticalArrayBounds(fromIndex, toIndex, array.size)
    parallelPrefix0(array, fromIndex, toIndex, op)
  }

  private fun parallelPrefix0(
    array: IntArray,
    fromIndex: Int,
    toIndex: Int,
    op: IntBinaryOperator
  ) {
    requireNotNull(array)
    var acc: Int = array[fromIndex]
    for (i in fromIndex + 1 until toIndex) {
      acc = op.applyAsInt(acc, array[i])
      array[i] = acc
    }
  }

  fun parallelPrefix(array: LongArray?, op: LongBinaryOperator) {
    requireNotNull(array)
    parallelPrefix0(array, 0, array.size, op)
  }

  fun parallelPrefix(array: LongArray?, fromIndex: Int, toIndex: Int, op: LongBinaryOperator) {
    requireNotNull(array)
    checkCriticalArrayBounds(fromIndex, toIndex, array.size)
    parallelPrefix0(array, fromIndex, toIndex, op)
  }

  private fun parallelPrefix0(
    array: LongArray,
    fromIndex: Int,
    toIndex: Int,
    op: LongBinaryOperator
  ) {
    requireNotNull(array)
    var acc: Long = array[fromIndex]
    for (i in fromIndex + 1 until toIndex) {
      acc = op.applyAsLong(acc, array[i])
      array[i] = acc
    }
  }

  fun <T> parallelPrefix(array: Array<T>?, op: BinaryOperator<T>) {
    requireNotNull(array)
    parallelPrefix0(array, 0, array.size, op)
  }

  fun <T> parallelPrefix(array: Array<T>?, fromIndex: Int, toIndex: Int, op: BinaryOperator<T>) {
    requireNotNull(array)
    checkCriticalArrayBounds(fromIndex, toIndex, array.size)
    parallelPrefix0(array, fromIndex, toIndex, op)
  }

  private fun <T> parallelPrefix0(
    array: Array<T>,
    fromIndex: Int,
    toIndex: Int,
    op: BinaryOperator<T>
  ) {
    requireNotNull(op)
    var acc = array[fromIndex]
    for (i in fromIndex + 1 until toIndex) {
      acc = op.apply(acc, array[i])!!
      array[i] = acc
    }
  }

  fun <T> setAll(array: Array<T>?, generator: IntFunction<out T>) {
    requireNotNull(array)
    requireNotNull(generator)
    for (i in array.indices) {
      array[i] = generator.apply(i)!!
    }
  }

  fun setAll(array: DoubleArray?, generator: IntToDoubleFunction) {
    requireNotNull(array)
    for (i in array.indices) {
      array[i] = generator.applyAsDouble(i)
    }
  }

  fun setAll(array: IntArray?, generator: IntUnaryOperator) {
    requireNotNull(array)
    for (i in array.indices) {
      array[i] = generator.applyAsInt(i)
    }
  }

  fun setAll(array: LongArray?, generator: IntToLongFunction) {
    requireNotNull(array)
    for (i in array.indices) {
      array[i] = generator.applyAsLong(i)
    }
  }

  fun <T> parallelSetAll(array: Array<T>?, generator: IntFunction<out T>) {
    requireNotNull(array)
    setAll(array, generator)
  }

  fun parallelSetAll(array: DoubleArray?, generator: IntToDoubleFunction) {
    requireNotNull(array)
    setAll(array, generator)
  }

  fun parallelSetAll(array: IntArray?, generator: IntUnaryOperator) {
    requireNotNull(array)
    setAll(array, generator)
  }

  fun parallelSetAll(array: LongArray?, generator: IntToLongFunction) {
    requireNotNull(array)
    setAll(array, generator)
  }

  fun sort(array: ByteArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun sort(array: ByteArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun sort(array: CharArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun sort(array: CharArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun sort(array: DoubleArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun sort(array: DoubleArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun sort(array: FloatArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun sort(array: FloatArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun sort(array: IntArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun sort(array: IntArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun sort(array: LongArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun sort(array: LongArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun sort(array: ShortArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun sort(array: ShortArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun <T> sort(x: Array<T>, c: Comparator<in T>) {
    x.sortWith(c)
  }

  fun <T> sort(x: Array<T>, fromIndex: Int, toIndex: Int, c: Comparator<in T>) {
    x.sortWith(c, fromIndex, toIndex)
  }

  fun parallelSort(array: ByteArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun parallelSort(array: ByteArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun parallelSort(array: CharArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun parallelSort(array: CharArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun parallelSort(array: DoubleArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun parallelSort(array: DoubleArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun parallelSort(array: FloatArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun parallelSort(array: FloatArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun parallelSort(array: IntArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun parallelSort(array: IntArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun parallelSort(array: LongArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun parallelSort(array: LongArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun parallelSort(array: ShortArray?) {
    requireNotNull(array)
    array.sort()
  }

  fun parallelSort(array: ShortArray?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun <T : Comparable<T>> parallelSort(array: Array<T>?) {
    requireNotNull(array)
    array.sort()
  }

  fun <T> parallelSort(array: Array<T>?, c: Comparator<in T>) {
    requireNotNull(array)
    array.sortWith(c)
  }

  fun <T : Comparable<T>> parallelSort(array: Array<T>?, fromIndex: Int, toIndex: Int) {
    requireNotNull(array)
    array.sort(fromIndex, toIndex)
  }

  fun <T> parallelSort(array: Array<T>?, fromIndex: Int, toIndex: Int, c: Comparator<in T>) {
    requireNotNull(array)
    array.sortWith(c, fromIndex, toIndex)
  }
}
