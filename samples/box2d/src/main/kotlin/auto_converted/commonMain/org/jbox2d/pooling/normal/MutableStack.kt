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
package org.jbox2d.pooling.normal

import org.jbox2d.pooling.IDynamicStack

abstract class MutableStack<E>(argInitSize: Int) : IDynamicStack<E> {
  // TODO(b/242202958): use a late-initialized variable.
  private var stack: Array<Any?>? = null
  private var index = 0
  private var size = 0

  init {
    index = 0
    extendStack(argInitSize)
  }

  private fun extendStack(argSize: Int) {
    val newStack = arrayOfNulls<Any>(argSize)
    if (stack != null) {
      stack!!.copyInto(newStack, 0, 0, size)
    }
    for (i in newStack.indices) {
      newStack[i] = newInstance()
    }
    stack = newStack
    size = newStack.size
  }

  override fun pop(): E {
    if (index >= size) {
      extendStack(size * 2)
    }
    @Suppress("UNCHECKED_CAST") return stack!![index++] as E
  }

  override fun push(argObject: E) {
    // assert is not supported in KMP.
    // assert(index > 0)
    stack!![--index] = argObject
  }

  /** Creates a new instance of the object contained by this stack. */
  protected abstract fun newInstance(): E
}
