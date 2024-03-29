/*
 * ****************************************************************************
 * Copyright (c) 2013, Daniel Murphy All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * JBox2D - A Java Port of Erin Catto's Box2D
 *
 * JBox2D homepage: http://jbox2d.sourceforge.net/
 * Box2D homepage: http://www.box2d.org
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 * claim that you wrote the original software. If you use this software
 * in a product, an acknowledgment in the product documentation would be
 * appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 * misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 ******************************************************************************/
package org.jbox2d.collision

/**
 * Contact ids to facilitate warm starting. Note: the ContactFeatures class is just embedded in here
 */
class ContactID : Comparable<ContactID> {
  enum class Type {
    VERTEX,
    FACE
  }

  var indexA: Byte = 0
  var indexB: Byte = 0
  var typeA: Byte = 0
  var typeB: Byte = 0
  val key: Int
    get() =
      indexA.toInt() shl 24 or (indexB.toInt() shl 16) or (typeA.toInt() shl 8) or typeB.toInt()

  constructor() {}
  constructor(c: ContactID) {
    set(c)
  }

  fun isEqual(cid: ContactID): Boolean = key == cid.key

  fun set(c: ContactID) {
    indexA = c.indexA
    indexB = c.indexB
    typeA = c.typeA
    typeB = c.typeB
  }

  fun flip() {
    var tempA = indexA
    indexA = indexB
    indexB = tempA
    tempA = typeA
    typeA = typeB
    typeB = tempA
  }

  /** zeros out the data */
  fun zero() {
    indexA = 0
    indexB = 0
    typeA = 0
    typeB = 0
  }

  override fun compareTo(other: ContactID): Int = key - other.key
}
