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
package org.jbox2d.common

import org.jbox2d.common.Mat22.Companion.createScaleTransform

/**
 * Orientated bounding box viewport transform
 *
 * @author Daniel Murphy
 */
class OBBViewportTransform : IViewportTransform {
  class OBB {
    val R = Mat22()
    val center = Vec2()
    val extents = Vec2()
  }

  var transform: Mat22
    get() = box.R
    set(transform) {
      box.R.set(transform)
    }

  protected val box = OBB().apply { R.setIdentity() }
  private var yFlip = false
  private val yFlipMat = Mat22(1f, 0f, 0f, -1f)
  private val yFlipMatInv = yFlipMat.invert()
  private val inv2 = Mat22()
  // djm pooling
  private val inv = Mat22()

  fun set(vpt: OBBViewportTransform) {
    box.center.set(vpt.box.center)
    box.extents.set(vpt.box.extents)
    box.R.set(vpt.box.R)
    yFlip = vpt.yFlip
  }

  /** @see IViewportTransform.setCamera */
  override fun setCamera(x: Float, y: Float, scale: Float) {
    box.center.set(x, y)
    createScaleTransform(scale, box.R)
  }

  /** @see IViewportTransform.getExtents */
  override fun getExtents(): Vec2 = box.extents

  /** @see IViewportTransform.setExtents */
  override fun setExtents(argExtents: Vec2) {
    box.extents.set(argExtents)
  }

  /** @see IViewportTransform.setExtents */
  override fun setExtents(argHalfWidth: Float, argHalfHeight: Float) {
    box.extents.set(argHalfWidth, argHalfHeight)
  }

  /** @see IViewportTransform.getCenter */
  override fun getCenter(): Vec2 = box.center

  /** @see IViewportTransform.setCenter */
  override fun setCenter(argPos: Vec2) {
    box.center.set(argPos)
  }

  /** @see IViewportTransform.setCenter */
  override fun setCenter(x: Float, y: Float) {
    box.center.set(x, y)
  }

  /**
   * Multiplies the obb transform by the given transform
   *
   * @param argTransform
   */
  fun mulByTransform(argTransform: Mat22) {
    box.R.mulLocal(argTransform)
  }

  /** @see IViewportTransform.isYFlip */
  override fun isYFlip(): Boolean = yFlip

  /** @see IViewportTransform.setYFlip */
  override fun setYFlip(yFlip: Boolean) {
    this.yFlip = yFlip
  }

  /** @see IViewportTransform.getScreenVectorToWorld */
  override fun getScreenVectorToWorld(argScreen: Vec2, argWorld: Vec2) {
    inv.set(box.R)
    inv.invertLocal()
    inv.mulToOut(argScreen, argWorld)
    if (yFlip) {
      yFlipMatInv.mulToOut(argWorld, argWorld)
    }
  }

  /** @see IViewportTransform.getWorldVectorToScreen */
  override fun getWorldVectorToScreen(argWorld: Vec2, argScreen: Vec2) {
    box.R.mulToOut(argWorld, argScreen)
    if (yFlip) {
      yFlipMatInv.mulToOut(argScreen, argScreen)
    }
  }

  override fun getWorldToScreen(argWorld: Vec2, argScreen: Vec2) {
    argScreen.x = argWorld.x - box.center.x
    argScreen.y = argWorld.y - box.center.y
    box.R.mulToOut(argScreen, argScreen)
    if (yFlip) {
      yFlipMat.mulToOut(argScreen, argScreen)
    }
    argScreen.x += box.extents.x
    argScreen.y += box.extents.y
  }

  /** @see IViewportTransform.getScreenToWorld */
  override fun getScreenToWorld(argScreen: Vec2, argWorld: Vec2) {
    argWorld.set(argScreen)
    argWorld.subLocal(box.extents)
    box.R.invertToOut(inv2)
    inv2.mulToOut(argWorld, argWorld)
    if (yFlip) {
      yFlipMatInv.mulToOut(argWorld, argWorld)
    }
    argWorld.addLocal(box.center)
  }
}
