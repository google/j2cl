/*
 * Copyright 2009 Google Inc.
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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * INCLUDES MODIFICATIONS BY RICHARD ZSCHECH AS WELL AS GOOGLE.
 */
package java.util

import kotlin.jvm.synchronized
import kotlin.math.ln
import kotlin.math.sqrt

class Random {
  var ktRandom: kotlin.random.Random
  var haveNextNextGaussian = false
  var nextNextGaussian = 0.0

  constructor() {
    ktRandom = kotlin.random.Random.Default
  }

  constructor(seed: Long) {
    ktRandom = kotlin.random.Random(seed)
  }

  fun nextBoolean() = ktRandom.nextBoolean()

  fun nextBytes(buf: ByteArray) = ktRandom.nextBytes(buf)

  fun nextDouble() = ktRandom.nextDouble()

  fun nextFloat() = ktRandom.nextFloat()

  fun nextGaussian(): Double {
    return synchronized(this) {
      if (haveNextNextGaussian) {
        // if X1 has been returned, return the second Gaussian
        haveNextNextGaussian = false
        nextNextGaussian
      } else {
        var s: Double
        var v1: Double
        var v2: Double
        do {
          // Generates two independent random variables U1, U2
          v1 = 2 * nextDouble() - 1
          v2 = 2 * nextDouble() - 1
          s = v1 * v1 + v2 * v2
        } while (s >= 1)

        // See errata for TAOCP vol. 2, 3rd ed. for proper handling of s == 0 case
        // (page 5 of http://www-cs-faculty.stanford.edu/~uno/err2.ps.gz)
        val norm = if (s == 0.0) 0.0 else sqrt(-2.0 * ln(s) / s)
        nextNextGaussian = v2 * norm
        haveNextNextGaussian = true
        v1 * norm
      }
    }
  }

  fun nextInt() = ktRandom.nextInt()

  fun nextInt(n: Int) = ktRandom.nextInt(n)

  fun nextLong() = ktRandom.nextLong()

  fun setSeed(seed: Long) {
    synchronized(this) {
      haveNextNextGaussian = false
      ktRandom = kotlin.random.Random(seed)
    }
  }
}
