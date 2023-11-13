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
package org.jbox2d.collision.shapes

import org.jbox2d.collision.AABB
import org.jbox2d.collision.RayCastInput
import org.jbox2d.collision.RayCastOutput
import org.jbox2d.common.Settings
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2

/**
 * A line segment (edge) shape. These can be connected in chains or loops to other edge shapes. The
 * connectivity information is used to ensure correct contact normals.
 *
 * @author Daniel
 */
class EdgeShape : Shape(ShapeType.EDGE) {
  /** edge vertex 1 */
  val m_vertex1 = Vec2()

  /** edge vertex 2 */
  val m_vertex2 = Vec2()

  /** optional adjacent vertex 1. Used for smooth collision */
  val m_vertex0 = Vec2()

  /** optional adjacent vertex 2. Used for smooth collision */
  val m_vertex3 = Vec2()
  var m_hasVertex0 = false
  var m_hasVertex3 = false

  init {
    m_radius = Settings.polygonRadius
  }

  override fun getChildCount(): Int {
    return 1
  }

  operator fun set(v1: Vec2, v2: Vec2) {
    m_vertex1.set(v1)
    m_vertex2.set(v2)
    m_hasVertex3 = false
    m_hasVertex0 = m_hasVertex3
  }

  override fun testPoint(xf: Transform, p: Vec2): Boolean {
    return false
  }

  // for pooling
  private val normal = Vec2()

  override fun raycast(
    output: RayCastOutput,
    input: RayCastInput,
    transform: Transform,
    childIndex: Int
  ): Boolean {
    var tempx: Float
    var tempy: Float
    val v1 = m_vertex1
    val v2 = m_vertex2
    val xfq = transform.q
    val xfp = transform.p

    // Put the ray into the edge's frame of reference.
    // b2Vec2 p1 = b2MulT(xf.q, input.p1 - xf.p);
    // b2Vec2 p2 = b2MulT(xf.q, input.p2 - xf.p);
    tempx = input.p1.x - xfp.x
    tempy = input.p1.y - xfp.y
    val p1x: Float = xfq.cos * tempx + xfq.sin * tempy
    val p1y: Float = -xfq.sin * tempx + xfq.cos * tempy
    tempx = input.p2.x - xfp.x
    tempy = input.p2.y - xfp.y
    val p2x: Float = xfq.cos * tempx + xfq.sin * tempy
    val p2y: Float = -xfq.sin * tempx + xfq.cos * tempy
    val dx = p2x - p1x
    val dy = p2y - p1y

    // final Vec2 normal = pool2.set(v2).subLocal(v1);
    // normal.set(normal.y, -normal.x);
    normal.x = v2.y - v1.y
    normal.y = v1.x - v2.x
    normal.normalize()
    val normalx = normal.x
    val normaly = normal.y

    // q = p1 + t * d
    // dot(normal, q - v1) = 0
    // dot(normal, p1 - v1) + t * dot(normal, d) = 0
    tempx = v1.x - p1x
    tempy = v1.y - p1y
    val numerator = normalx * tempx + normaly * tempy
    val denominator = normalx * dx + normaly * dy
    if (denominator == 0.0f) {
      return false
    }
    val t = numerator / denominator
    if (t < 0.0f || 1.0f < t) {
      return false
    }

    // Vec2 q = p1 + t * d;
    val qx = p1x + t * dx
    val qy = p1y + t * dy

    // q = v1 + s * r
    // s = dot(q - v1, r) / dot(r, r)
    // Vec2 r = v2 - v1;
    val rx = v2.x - v1.x
    val ry = v2.y - v1.y
    val rr = rx * rx + ry * ry
    if (rr == 0.0f) {
      return false
    }
    tempx = qx - v1.x
    tempy = qy - v1.y
    // float s = Vec2.dot(pool5, r) / rr;
    val s = (tempx * rx + tempy * ry) / rr
    if (s < 0.0f || 1.0f < s) {
      return false
    }
    output.fraction = t
    if (numerator > 0.0f) {
      // argOutput.normal = -normal;
      output.normal.x = -normalx
      output.normal.y = -normaly
    } else {
      // output.normal = normal;
      output.normal.x = normalx
      output.normal.y = normaly
    }
    return true
  }

  override fun computeAABB(aabb: AABB, xf: Transform, childIndex: Int) {
    val lowerBound = aabb.lowerBound
    val upperBound = aabb.upperBound
    val xfq = xf.q
    val v1x: Float = xfq.cos * m_vertex1.x - xfq.sin * m_vertex1.y + xf.p.x
    val v1y: Float = xfq.sin * m_vertex1.x + xfq.cos * m_vertex1.y + xf.p.y
    val v2x: Float = xfq.cos * m_vertex2.x - xfq.sin * m_vertex2.y + xf.p.x
    val v2y: Float = xfq.sin * m_vertex2.x + xfq.cos * m_vertex2.y + xf.p.y
    lowerBound.x = if (v1x < v2x) v1x else v2x
    lowerBound.y = if (v1y < v2y) v1y else v2y
    upperBound.x = if (v1x > v2x) v1x else v2x
    upperBound.y = if (v1y > v2y) v1y else v2y
    lowerBound.x -= m_radius
    lowerBound.y -= m_radius
    upperBound.x += m_radius
    upperBound.y += m_radius
  }

  override fun computeMass(massData: MassData, density: Float) {
    massData.mass = 0.0f
    massData.center.set(m_vertex1).addLocal(m_vertex2).mulLocal(0.5f)
    massData.I = 0.0f
  }

  override fun clone(): Shape {
    val edge = EdgeShape()
    edge.m_radius = m_radius
    edge.m_hasVertex0 = m_hasVertex0
    edge.m_hasVertex3 = m_hasVertex3
    edge.m_vertex0.set(m_vertex0)
    edge.m_vertex1.set(m_vertex1)
    edge.m_vertex2.set(m_vertex2)
    edge.m_vertex3.set(m_vertex3)
    return edge
  }
}
