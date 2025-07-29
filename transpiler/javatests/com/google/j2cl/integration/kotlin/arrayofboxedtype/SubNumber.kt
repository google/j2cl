/*
 * Copyright 2022 Google Inc.
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
package arrayofboxedtype

class SubNumber : Number() {
  override fun toByte(): Byte = 0

  override fun toChar(): Char = 'a'

  override fun toDouble(): Double = 0.0

  override fun toFloat(): Float = 0.0f

  override fun toInt(): Int = 0

  override fun toLong(): Long = 0L

  override fun toShort(): Short = 0
}
