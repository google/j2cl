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
package java.lang

import kotlin.native.identityHashCode
import kotlin.system.getTimeMillis
import kotlin.system.getTimeNanos

// TODO(b/224765929): Avoid this hack for InternalPreconditions.java.
object System {
  fun getProperty(name: String?): String? =
    when (name) {
      "jre.checks.api",
      "jre.checks.bounds",
      "jre.checks.numeric",
      "jre.checks.type" -> "AUTO"
      "jre.checks.checkLevel" -> "NORMAL"
      else -> null
    }

  fun arraycopy(src: Any?, srcOfs: Int, dest: Any?, destOfs: Int, len: Int) {
    when (src) {
      is ByteArray -> src.copyInto(dest as ByteArray, destOfs, srcOfs, srcOfs + len)
      is ShortArray -> src.copyInto(dest as ShortArray, destOfs, srcOfs, srcOfs + len)
      is IntArray -> src.copyInto(dest as IntArray, destOfs, srcOfs, srcOfs + len)
      is LongArray -> src.copyInto(dest as LongArray, destOfs, srcOfs, srcOfs + len)
      is FloatArray -> src.copyInto(dest as FloatArray, destOfs, srcOfs, srcOfs + len)
      is DoubleArray -> src.copyInto(dest as DoubleArray, destOfs, srcOfs, srcOfs + len)
      is BooleanArray -> src.copyInto(dest as BooleanArray, destOfs, srcOfs, srcOfs + len)
      is CharArray -> src.copyInto(dest as CharArray, destOfs, srcOfs, srcOfs + len)
      is Array<*> -> src.copyInto(dest as Array<Any?>, destOfs, srcOfs, srcOfs + len)
      else -> throw ArrayStoreException()
    }
  }

  fun currentTimeMillis(): Long = getTimeMillis()

  fun nanoTime(): Long = getTimeNanos()

  fun gc(): Unit = Unit

  fun identityHashCode(o: Any?): Int = o.identityHashCode()
}
