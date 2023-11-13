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
 * ****************************************************************************
 */
/** Created at 4:35:29 AM Jul 15, 2010 */
package org.jbox2d.callbacks

import org.jbox2d.common.Color3f
import org.jbox2d.common.IViewportTransform
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2

// updated to rev 100
/**
 * Implement this abstract class to allow JBox2d to automatically draw your physics for debugging
 * purposes. Not intended to replace your own custom rendering routines!
 *
 * @author Daniel Murphy
 */
abstract class DebugDraw(val viewportTransform: IViewportTransform) {
  var flags: Int = 0

  fun appendFlags(flags: Int) {
    this.flags = this.flags or flags
  }

  fun clearFlags(flags: Int) {
    this.flags = this.flags and flags.inv()
  }

  /**
   * Draw a closed polygon provided in CCW order. This implementation uses [.drawSegment] to draw
   * each side of the polygon.
   *
   * @param vertices
   * @param vertexCount
   * @param color
   */
  fun drawPolygon(vertices: Array<Vec2>, vertexCount: Int, color: Color3f) {
    if (vertexCount == 1) {
      drawSegment(vertices[0], vertices[0], color)
      return
    }
    for (i in 0 until vertexCount - 1) {
      drawSegment(vertices[i], vertices[i + 1], color)
    }
    if (vertexCount > 2) {
      drawSegment(vertices[vertexCount - 1], vertices[0], color)
    }
  }

  abstract fun drawPoint(argPoint: Vec2, argRadiusOnScreen: Float, argColor: Color3f)

  /**
   * Draw a solid closed polygon provided in CCW order.
   *
   * @param vertices
   * @param vertexCount
   * @param color
   */
  abstract fun drawSolidPolygon(vertices: Array<Vec2>, vertexCount: Int, color: Color3f)

  /**
   * Draw a circle.
   *
   * @param center
   * @param radius
   * @param color
   */
  abstract fun drawCircle(center: Vec2, radius: Float, color: Color3f)

  /**
   * Draw a solid circle.
   *
   * @param center
   * @param radius
   * @param axis
   * @param color
   */
  abstract fun drawSolidCircle(center: Vec2, radius: Float, axis: Vec2, color: Color3f)

  /**
   * Draw a line segment.
   *
   * @param p1
   * @param p2
   * @param color
   */
  abstract fun drawSegment(p1: Vec2, p2: Vec2, color: Color3f)

  /**
   * Draw a transform. Choose your own length scale
   *
   * @param xf
   */
  abstract fun drawTransform(xf: Transform)

  /**
   * Draw a string.
   *
   * @param x
   * @param y
   * @param s
   * @param color
   */
  abstract fun drawString(x: Float, y: Float, s: String, color: Color3f)

  fun drawString(pos: Vec2, s: String, color: Color3f) {
    drawString(pos.x, pos.y, s, color)
  }

  /**
   * @param x
   * @param y
   * @param scale
   * @see IViewportTransform.setCamera
   */
  fun setCamera(x: Float, y: Float, scale: Float) {
    viewportTransform.setCamera(x, y, scale)
  }

  /**
   * @param argScreen
   * @param argWorld
   * @see org.jbox2d.common.IViewportTransform.getScreenToWorld
   */
  fun getScreenToWorldToOut(argScreen: Vec2, argWorld: Vec2) {
    viewportTransform.getScreenToWorld(argScreen, argWorld)
  }

  /**
   * @param argWorld
   * @param argScreen
   * @see org.jbox2d.common.IViewportTransform.getWorldToScreen
   */
  fun getWorldToScreenToOut(argWorld: Vec2, argScreen: Vec2) {
    viewportTransform.getWorldToScreen(argWorld, argScreen)
  }

  /**
   * Takes the world coordinates and puts the corresponding screen coordinates in argScreen.
   *
   * @param worldX
   * @param worldY
   * @param argScreen
   */
  fun getWorldToScreenToOut(worldX: Float, worldY: Float, argScreen: Vec2) {
    argScreen.set(worldX, worldY)
    viewportTransform.getWorldToScreen(argScreen, argScreen)
  }

  /**
   * takes the world coordinate (argWorld) and returns the screen coordinates.
   *
   * @param argWorld
   */
  fun getWorldToScreen(argWorld: Vec2): Vec2 {
    val screen = Vec2()
    viewportTransform.getWorldToScreen(argWorld, screen)
    return screen
  }

  /**
   * Takes the world coordinates and returns the screen coordinates.
   *
   * @param worldX
   * @param worldY
   */
  fun getWorldToScreen(worldX: Float, worldY: Float): Vec2 {
    val argScreen = Vec2(worldX, worldY)
    viewportTransform.getWorldToScreen(argScreen, argScreen)
    return argScreen
  }

  /**
   * takes the screen coordinates and puts the corresponding world coordinates in argWorld.
   *
   * @param screenX
   * @param screenY
   * @param argWorld
   */
  fun getScreenToWorldToOut(screenX: Float, screenY: Float, argWorld: Vec2) {
    argWorld.set(screenX, screenY)
    viewportTransform.getScreenToWorld(argWorld, argWorld)
  }

  /**
   * takes the screen coordinates (argScreen) and returns the world coordinates
   *
   * @param argScreen
   */
  fun getScreenToWorld(argScreen: Vec2): Vec2 {
    val world = Vec2()
    viewportTransform.getScreenToWorld(argScreen, world)
    return world
  }

  /**
   * takes the screen coordinates and returns the world coordinates.
   *
   * @param screenX
   * @param screenY
   */
  fun getScreenToWorld(screenX: Float, screenY: Float): Vec2 {
    val screen = Vec2(screenX, screenY)
    viewportTransform.getScreenToWorld(screen, screen)
    return screen
  }

  companion object {
    const val E_SHAPE_BIT: Int = 0x0001 // /< draw shapes
    const val E_JOINT_BIT: Int = 0x0002 // /< draw joint connections
    const val E_AABB_BIT: Int = 0x0004 // /< draw core (TOI) shapes
    const val E_PAIR_BIT: Int = 0x0008 // /< draw axis aligned bounding boxes
    const val E_CENTER_OF_MASS_BIT: Int = 0x0010 // /< draw center of mass frame
    const val E_DYNAMIC_TREE_BIT: Int = 0x0020 // /< draw dynamic tree.
  }
}
