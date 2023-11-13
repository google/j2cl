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
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.pooling.arrays.Vec2Array

/**
 * A convex polygon shape. Polygons have a maximum number of vertices equal to _maxPolygonVertices.
 * In most cases you should not need many vertices for a convex polygon.
 */
class PolygonShape : Shape(ShapeType.POLYGON) {
  /** Local position of the shape centroid in parent body frame. */
  val centroid: Vec2 = Vec2().apply { this.setZero() }

  /**
   * The vertices of the shape. Note: use getVertexCount(), not m_vertices.length, to get number of
   * active vertices.
   */
  val vertices: Array<Vec2> = Array<Vec2>(Settings.MAX_POLYGON_VERTICES) { Vec2() }

  /**
   * The normals of the shape. Note: use getVertexCount(), not m_normals.length, to get number of
   * active normals.
   */
  val normals: Array<Vec2> = Array<Vec2>(Settings.MAX_POLYGON_VERTICES) { Vec2() }

  /** Number of active vertices in the shape. */
  var count = 0

  // pooling
  private val pool1 = Vec2()
  private val pool2 = Vec2()
  private val pool3 = Vec2()
  private val pool4 = Vec2()
  private val poolt1 = Transform()

  override var radius = Settings.POLYGON_RADIUS

  override fun clone(): Shape {
    val shape = PolygonShape()
    shape.centroid.set(centroid)
    for (i in shape.normals.indices) {
      shape.normals[i].set(normals[i])
      shape.vertices[i].set(vertices[i])
    }
    shape.radius = this.radius
    shape.count = count
    return shape
  }

  /**
   * Create a convex hull from the given array of points. The count must be in the range
   * [3, Settings.maxPolygonVertices].
   *
   * @warning the points may be re-ordered, even if they form a convex polygon
   * @warning collinear points are handled but not removed. Collinear points may lead to poor
   *   stacking behavior.
   */
  fun set(vertices: Array<Vec2>, count: Int) {
    set(vertices, count, null, null)
  }

  /**
   * Create a convex hull from the given array of points. The count must be in the range
   * [3, Settings.maxPolygonVertices]. This method takes an arraypool for pooling
   *
   * @warning the points may be re-ordered, even if they form a convex polygon
   * @warning collinear points are handled but not removed. Collinear points may lead to poor
   *   stacking behavior.
   */
  fun set(
    verts: Array<Vec2>,
    num: Int,
    vecPool: Vec2Array?,
    intPool: org.jbox2d.pooling.arrays.IntArray?
  ) {
    // assert is not supported in KMP.
    // assert(3 <= num && num <= Settings.maxPolygonVertices)
    if (num < 3) {
      setAsBox(1.0f, 1.0f)
      return
    }
    val n = MathUtils.min(num, Settings.MAX_POLYGON_VERTICES)

    // Copy the vertices into a local buffer
    val ps: Array<Vec2> = vecPool?.get(n) ?: Array<Vec2>(n) { index -> verts[index] }

    // Create the convex hull using the Gift wrapping algorithm
    // http://en.wikipedia.org/wiki/Gift_wrapping_algorithm

    // Find the right most point on the hull
    var i0 = 0
    var x0 = ps[0].x
    for (i in 1 until num) {
      val x = ps[i].x
      if (x > x0 || (x == x0 && ps[i].y < ps[i0].y)) {
        i0 = i
        x0 = x
      }
    }
    val hull =
      intPool?.get(Settings.MAX_POLYGON_VERTICES) ?: IntArray(Settings.MAX_POLYGON_VERTICES)
    var m = 0
    var ih = i0
    while (true) {
      hull[m] = ih
      var ie = 0
      for (j in 1 until n) {
        if (ie == ih) {
          ie = j
          continue
        }
        val r = pool1.set(ps[ie]).subLocal(ps[hull[m]])
        val v = pool2.set(ps[j]).subLocal(ps[hull[m]])
        val c = Vec2.cross(r, v)
        if (c < 0.0f) {
          ie = j
        }

        // Collinearity check
        if (c == 0.0f && v.lengthSquared() > r.lengthSquared()) {
          ie = j
        }
      }
      ++m
      ih = ie
      if (ie == i0) {
        break
      }
    }
    count = m

    // Copy vertices.
    for (i in 0 until count) {
      vertices[i].set(ps[hull[i]])
    }
    val edge = pool1

    // Compute normals. Ensure the edges have non-zero length.
    for (i in 0 until count) {
      val i2 = if (i + 1 < count) i + 1 else 0
      edge.set(vertices[i2]).subLocal(vertices[i])
      // assert is not supported in KMP.
      // assert(edge.lengthSquared() > Settings.EPSILON * Settings.EPSILON)
      Vec2.crossToOutUnsafe(edge, 1f, normals[i])
      normals[i].normalize()
    }

    // Compute the polygon centroid.
    computeCentroidToOut(vertices, count, centroid)
  }

  /**
   * Build vertices to represent an axis-aligned box.
   *
   * @param hx the half-width.
   * @param hy the half-height.
   */
  fun setAsBox(hx: Float, hy: Float) {
    count = 4
    vertices[0].set(-hx, -hy)
    vertices[1].set(hx, -hy)
    vertices[2].set(hx, hy)
    vertices[3].set(-hx, hy)
    normals[0].set(0.0f, -1.0f)
    normals[1].set(1.0f, 0.0f)
    normals[2].set(0.0f, 1.0f)
    normals[3].set(-1.0f, 0.0f)
    centroid.setZero()
  }

  /**
   * Build vertices to represent an oriented box.
   *
   * @param hx the half-width.
   * @param hy the half-height.
   * @param center the center of the box in local coordinates.
   * @param angle the rotation of the box in local coordinates.
   */
  fun setAsBox(hx: Float, hy: Float, center: Vec2, angle: Float) {
    count = 4
    vertices[0].set(-hx, -hy)
    vertices[1].set(hx, -hy)
    vertices[2].set(hx, hy)
    vertices[3].set(-hx, hy)
    normals[0].set(0.0f, -1.0f)
    normals[1].set(1.0f, 0.0f)
    normals[2].set(0.0f, 1.0f)
    normals[3].set(-1.0f, 0.0f)
    centroid.set(center)
    val xf = poolt1
    xf.p.set(center)
    xf.q.set(angle)

    // Transform vertices and normals.
    for (i in 0 until count) {
      Transform.mulToOut(xf, vertices[i], vertices[i])
      Rot.mulToOut(xf.q, normals[i], normals[i])
    }
  }

  override fun getChildCount(): Int = 1

  override fun testPoint(xf: Transform, p: Vec2): Boolean {
    var tempx: Float
    var tempy: Float
    val xfq = xf.q
    tempx = p.x - xf.p.x
    tempy = p.y - xf.p.y
    val pLocalx: Float = xfq.cos * tempx + xfq.sin * tempy
    val pLocaly: Float = -xfq.sin * tempx + xfq.cos * tempy
    if (DEBUG) {
      println("--testPoint debug--")
      println("Vertices: ")
      for (i in 0 until count) {
        println(vertices[i])
      }
      println("pLocal: $pLocalx, $pLocaly")
    }
    for (i in 0 until count) {
      val vertex = vertices[i]
      val normal = normals[i]
      tempx = pLocalx - vertex.x
      tempy = pLocaly - vertex.y
      val dot = normal.x * tempx + normal.y * tempy
      if (dot > 0.0f) {
        return false
      }
    }
    return true
  }

  override fun computeAABB(aabb: AABB, xf: Transform, childIndex: Int) {
    val lower = aabb.lowerBound
    val upper = aabb.upperBound
    val v1 = vertices[0]
    val xfq = xf.q
    val xfp = xf.p
    var vx: Float
    var vy: Float
    lower.x = xfq.cos * v1.x - xfq.sin * v1.y + xfp.x
    lower.y = xfq.sin * v1.x + xfq.cos * v1.y + xfp.y
    upper.x = lower.x
    upper.y = lower.y
    for (i in 1 until count) {
      val v2 = vertices[i]
      // Vec2 v = Mul(xf, m_vertices[i]);
      vx = xfq.cos * v2.x - xfq.sin * v2.y + xfp.x
      vy = xfq.sin * v2.x + xfq.cos * v2.y + xfp.y
      lower.x = if (lower.x < vx) lower.x else vx
      lower.y = if (lower.y < vy) lower.y else vy
      upper.x = if (upper.x > vx) upper.x else vx
      upper.y = if (upper.y > vy) upper.y else vy
    }
    lower.x -= radius
    lower.y -= radius
    upper.x += radius
    upper.y += radius
  }

  /**
   * Get a vertex by index.
   *
   * @param index
   * @return
   */
  fun getVertex(index: Int): Vec2 = vertices[index]

  override fun raycast(
    output: RayCastOutput,
    input: RayCastInput,
    transform: Transform,
    childIndex: Int
  ): Boolean {
    val xfq = transform.q
    val xfp = transform.p
    var tempx: Float
    var tempy: Float
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
    var lower = 0f
    var upper = input.maxFraction
    var index = -1
    for (i in 0 until count) {
      val normal = normals[i]
      val vertex = vertices[i]
      // p = p1 + a * d
      // dot(normal, p - v) = 0
      // dot(normal, p1 - v) + a * dot(normal, d) = 0
      val tempxn = vertex.x - p1x
      val tempyn = vertex.y - p1y
      val numerator = normal.x * tempxn + normal.y * tempyn
      val denominator = normal.x * dx + normal.y * dy
      if (denominator == 0.0f) {
        if (numerator < 0.0f) {
          return false
        }
      } else {
        // Note: we want this predicate without division:
        // lower < numerator / denominator, where denominator < 0
        // Since denominator < 0, we have to flip the inequality:
        // lower < numerator / denominator <==> denominator * lower >
        // numerator.
        if (denominator < 0.0f && numerator < lower * denominator) {
          // Increase lower.
          // The segment enters this half-space.
          lower = numerator / denominator
          index = i
        } else if (denominator > 0.0f && numerator < upper * denominator) {
          // Decrease upper.
          // The segment exits this half-space.
          upper = numerator / denominator
        }
      }
      if (upper < lower) {
        return false
      }
    }
    // assert is not supported in KMP.
    // assert(0.0f <= lower && lower <= input.maxFraction)
    if (index >= 0) {
      output.fraction = lower
      // normal = Mul(xf.R, m_normals[index]);
      val normal = normals[index]
      val out = output.normal
      out.x = xfq.cos * normal.x - xfq.sin * normal.y
      out.y = xfq.sin * normal.x + xfq.cos * normal.y
      return true
    }
    return false
  }

  fun computeCentroidToOut(vs: Array<Vec2>, count: Int, out: Vec2) {
    // assert is not supported in KMP.
    // assert(count >= 3)
    out.set(0.0f, 0.0f)
    var area = 0.0f

    // pRef is the reference point for forming triangles.
    // It's location doesn't change the result (except for rounding error).
    val pRef = pool1
    pRef.setZero()
    val e1 = pool2
    val e2 = pool3
    val inv3 = 1.0f / 3.0f
    for (i in 0 until count) {
      // Triangle vertices.
      val p2 = vs[i]
      val p3 = if (i + 1 < count) vs[i + 1] else vs[0]
      e1.set(p2).subLocal(pRef)
      e2.set(p3).subLocal(pRef)
      val D = Vec2.cross(e1, e2)
      val triangleArea = 0.5f * D
      area += triangleArea

      // Area weighted centroid
      e1.set(pRef).addLocal(p2).addLocal(p3).mulLocal(triangleArea * inv3)
      out.addLocal(e1)
    }
    // assert is not supported in KMP.
    // assert(area > Settings.EPSILON)
    out.mulLocal(1.0f / area)
  }

  override fun computeMass(massData: MassData, density: Float) {
    // Polygon mass, centroid, and inertia.
    // Let rho be the polygon density in mass per unit area.
    // Then:
    // mass = rho * int(dA)
    // centroid.x = (1/mass) * rho * int(x * dA)
    // centroid.y = (1/mass) * rho * int(y * dA)
    // I = rho * int((x*x + y*y) * dA)
    //
    // We can compute these integrals by summing all the integrals
    // for each triangle of the polygon. To evaluate the integral
    // for a single triangle, we make a change of variables to
    // the (u,v) coordinates of the triangle:
    // x = x0 + e1x * u + e2x * v
    // y = y0 + e1y * u + e2y * v
    // where 0 <= u && 0 <= v && u + v <= 1.
    //
    // We integrate u from [0,1-v] and then v from [0,1].
    // We also need to use the Jacobian of the transformation:
    // D = cross(e1, e2)
    //
    // Simplification: triangle centroid = (1/3) * (p1 + p2 + p3)
    //
    // The rest of the derivation is handled by computer algebra.
    // assert is not supported in KMP.
    // assert(vertexCount >= 3)
    val center = pool1
    center.setZero()
    var area = 0.0f
    var I = 0.0f

    // pRef is the reference point for forming triangles.
    // It's location doesn't change the result (except for rounding error).
    val s = pool2
    s.setZero()
    // This code would put the reference point inside the polygon.
    for (i in 0 until count) {
      s.addLocal(vertices[i])
    }
    s.mulLocal(1.0f / count)
    val k_inv3 = 1.0f / 3.0f
    val e1 = pool3
    val e2 = pool4
    for (i in 0 until count) {
      // Triangle vertices.
      e1.set(vertices[i]).subLocal(s)
      e2.set(s).negateLocal().addLocal((if (i + 1 < count) vertices[i + 1] else vertices[0]))
      val D = Vec2.cross(e1, e2)
      val triangleArea = 0.5f * D
      area += triangleArea

      // Area weighted centroid
      center.x += triangleArea * k_inv3 * (e1.x + e2.x)
      center.y += triangleArea * k_inv3 * (e1.y + e2.y)
      val ex1 = e1.x
      val ey1 = e1.y
      val ex2 = e2.x
      val ey2 = e2.y
      val intx2 = ex1 * ex1 + ex2 * ex1 + ex2 * ex2
      val inty2 = ey1 * ey1 + ey2 * ey1 + ey2 * ey2
      I += 0.25f * k_inv3 * D * (intx2 + inty2)
    }

    // Total mass
    massData.mass = density * area
    // assert is not supported in KMP.
    // assert(area > Settings.EPSILON)
    center.mulLocal(1.0f / area)
    massData.center.set(center).addLocal(s)

    // Inertia tensor relative to the local origin (point s)
    massData.I = I * density

    // Shift to center of mass then to original body origin.
    massData.I += massData.mass * Vec2.dot(massData.center, massData.center)
  }

  /**
   * Validate convexity. This is a very time consuming operation.
   *
   * @return
   */
  fun validate(): Boolean {
    for (i in 0 until count) {
      val i2 = if (i < count - 1) i + 1 else 0
      val p = vertices[i]
      val e = pool1.set(vertices[i2]).subLocal(p)
      for (j in 0 until count) {
        if (j == i || j == i2) {
          continue
        }
        val v = pool2.set(vertices[j]).subLocal(p)
        val c = Vec2.cross(e, v)
        if (c < 0.0f) {
          return false
        }
      }
    }
    return true
  }

  /** Get the centroid and apply the supplied transform. */
  fun centroid(xf: Transform): Vec2 = Transform.mul(xf, centroid)

  /** Get the centroid and apply the supplied transform. */
  fun centroidToOut(xf: Transform, out: Vec2): Vec2 {
    Transform.mulToOutUnsafe(xf, centroid, out)
    return out
  }

  companion object {
    /** Dump lots of debug information. */
    private const val DEBUG = false
  }
}
