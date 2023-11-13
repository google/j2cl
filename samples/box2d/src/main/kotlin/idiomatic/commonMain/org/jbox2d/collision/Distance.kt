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
package org.jbox2d.collision

import org.jbox2d.collision.shapes.ChainShape
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.EdgeShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.collision.shapes.Shape
import org.jbox2d.collision.shapes.ShapeType
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2

// updated to rev 100
/**
 * This is non-static for faster pooling. To get an instance, use the [SingletonPool], don't
 * construct a distance object.
 *
 * @author Daniel Murphy
 */
class Distance {
  private val simplex = Simplex()
  private val saveA = IntArray(3)
  private val saveB = IntArray(3)
  private val closestPoint = Vec2()
  private val d = Vec2()
  private val temp = Vec2()
  private val normal = Vec2()

  /** GJK using Voronoi regions (Christer Ericson) and Barycentric coordinates. */
  private inner class SimplexVertex {
    val wA = Vec2() // support point in shapeA
    val wB = Vec2() // support point in shapeB
    val w = Vec2() // wB - wA
    var a = 0f // barycentric coordinate for closest point
    var indexA = 0 // wA index
    var indexB = 0 // wB index

    fun set(sv: SimplexVertex) {
      wA.set(sv.wA)
      wB.set(sv.wB)
      w.set(sv.w)
      a = sv.a
      indexA = sv.indexA
      indexB = sv.indexB
    }
  }

  /**
   * Used to warm start Distance. Set count to zero on first call.
   *
   * @author daniel
   */
  class SimplexCache {
    /** length or area */
    var metric = 0f
    var count = 0

    /** vertices on shape A */
    val indexA = IntArray(3) { Int.MAX_VALUE }

    /** vertices on shape B */
    val indexB = IntArray(3) { Int.MAX_VALUE }

    fun set(sc: SimplexCache) {
      sc.indexA.copyInto(indexA, 0, 0, indexA.size)
      sc.indexB.copyInto(indexB, 0, 0, indexA.size)
      metric = sc.metric
      count = sc.count
    }
  }

  private inner class Simplex {
    val v1 = SimplexVertex()
    val v2 = SimplexVertex()
    val v3 = SimplexVertex()
    val vertices = arrayOf(v1, v2, v3)
    var count = 0
    private val e12 = Vec2()

    // djm pooled
    private val case2 = Vec2()
    private val case22 = Vec2()

    // djm pooled, and from above
    private val case3 = Vec2()
    private val case33 = Vec2()

    // djm pooled, and from above
    private val e13 = Vec2()
    private val e23 = Vec2()
    private val w1 = Vec2()
    private val w2 = Vec2()
    private val w3 = Vec2()

    fun readCache(
      cache: SimplexCache,
      proxyA: DistanceProxy,
      transformA: Transform,
      proxyB: DistanceProxy,
      transformB: Transform
    ) {
      // assert is not supported in KMP.
      // assert(cache.count <= 3)

      // Copy data from cache.
      count = cache.count
      for (i in 0 until count) {
        val v = vertices[i]
        v.indexA = cache.indexA[i]
        v.indexB = cache.indexB[i]
        val wALocal = proxyA.vertices[v.indexA]
        val wBLocal = proxyB.vertices[v.indexB]
        Transform.mulToOutUnsafe(transformA, wALocal, v.wA)
        Transform.mulToOutUnsafe(transformB, wBLocal, v.wB)
        v.w.set(v.wB).subLocal(v.wA)
        v.a = 0.0f
      }

      // Compute the new simplex metric, if it is substantially different than
      // old metric then flush the simplex.
      if (count > 1) {
        val metric1 = cache.metric
        val metric2: Float = getMetric()
        if (metric2 < 0.5f * metric1 || 2.0f * metric1 < metric2 || metric2 < Settings.EPSILON) {
          // Reset the simplex.
          count = 0
        }
      }

      // If the cache is empty or invalid ...
      if (count == 0) {
        val v = vertices[0]
        v.indexA = 0
        v.indexB = 0
        val wALocal = proxyA.vertices[0]
        val wBLocal = proxyB.vertices[0]
        Transform.mulToOutUnsafe(transformA, wALocal, v.wA)
        Transform.mulToOutUnsafe(transformB, wBLocal, v.wB)
        v.w.set(v.wB).subLocal(v.wA)
        count = 1
      }
    }

    fun writeCache(cache: SimplexCache) {
      cache.metric = getMetric()
      cache.count = count
      for (i in 0 until count) {
        cache.indexA[i] = vertices[i].indexA
        cache.indexB[i] = vertices[i].indexB
      }
    }

    fun getSearchDirection(out: Vec2) {
      when (count) {
        1 -> {
          out.set(v1.w).negateLocal()
        }
        2 -> {
          e12.set(v2.w).subLocal(v1.w)
          // use out for a temp variable real quick
          out.set(v1.w).negateLocal()
          val sgn = Vec2.cross(e12, out)
          if (sgn > 0f) {
            // Origin is left of e12.
            Vec2.crossToOutUnsafe(1f, e12, out)
          } else {
            // Origin is right of e12.
            Vec2.crossToOutUnsafe(e12, 1f, out)
          }
        }
        else -> {
          // assert is not supported in KMP.
          // assert(false)
          out.setZero()
        }
      }
    }

    /**
     * this returns pooled objects. don't keep or modify them
     *
     * @return
     */
    fun getClosestPoint(out: Vec2) {
      when (count) {
        0 -> {
          // assert is not supported in KMP.
          // assert(false)
          out.setZero()
        }
        1 -> {
          out.set(v1.w)
        }
        2 -> {
          case22.set(v2.w).mulLocal(v2.a)
          case2.set(v1.w).mulLocal(v1.a).addLocal(case22)
          out.set(case2)
        }
        3 -> {
          out.setZero()
        }
        else -> {
          // assert is not supported in KMP.
          // assert(false)
          out.setZero()
        }
      }
    }

    fun getWitnessPoints(pA: Vec2, pB: Vec2) {
      when (count) {
        0 -> {
          // assert is not supported in KMP.
          // assert(false)
        }
        1 -> {
          pA.set(v1.wA)
          pB.set(v1.wB)
        }
        2 -> {
          case2.set(v1.wA).mulLocal(v1.a)
          pA.set(v2.wA).mulLocal(v2.a).addLocal(case2)
          // m_v1.a * m_v1.wA + m_v2.a * m_v2.wA;
          // *pB = m_v1.a * m_v1.wB + m_v2.a * m_v2.wB;
          case2.set(v1.wB).mulLocal(v1.a)
          pB.set(v2.wB).mulLocal(v2.a).addLocal(case2)
        }
        3 -> {
          pA.set(v1.wA).mulLocal(v1.a)
          case3.set(v2.wA).mulLocal(v2.a)
          case33.set(v3.wA).mulLocal(v3.a)
          pA.addLocal(case3).addLocal(case33)
          pB.set(pA)
        }
        else -> {
          // assert is not supported in KMP.
          // assert(false)
        }
      }
    }

    // djm pooled, from above
    fun getMetric(): Float {
      when (count) {
        0 -> {
          // assert is not supported in KMP.
          // assert(false)
          return 0.0f
        }
        1 -> return 0.0f
        2 -> return MathUtils.distance(v1.w, v2.w)
        3 -> {
          case3.set(v2.w).subLocal(v1.w)
          case33.set(v3.w).subLocal(v1.w)
          // return Vec2.cross(m_v2.w - m_v1.w, m_v3.w - m_v1.w);
          return Vec2.cross(case3, case33)
        }
        else -> {
          // assert is not supported in KMP.
          // assert(false)
          return 0.0f
        }
      }
    }

    // djm pooled from above
    /** Solve a line segment using barycentric coordinates. */
    fun solve2() {
      // Solve a line segment using barycentric coordinates.
      //
      // p = a1 * w1 + a2 * w2
      // a1 + a2 = 1
      //
      // The vector from the origin to the closest point on the line is
      // perpendicular to the line.
      // e12 = w2 - w1
      // dot(p, e) = 0
      // a1 * dot(w1, e) + a2 * dot(w2, e) = 0
      //
      // 2-by-2 linear system
      // [1 1 ][a1] = [1]
      // [w1.e12 w2.e12][a2] = [0]
      //
      // Define
      // d12_1 = dot(w2, e12)
      // d12_2 = -dot(w1, e12)
      // d12 = d12_1 + d12_2
      //
      // Solution
      // a1 = d12_1 / d12
      // a2 = d12_2 / d12
      val w1 = v1.w
      val w2 = v2.w
      e12.set(w2).subLocal(w1)

      // w1 region
      val d12_2 = -Vec2.dot(w1, e12)
      if (d12_2 <= 0.0f) {
        // a2 <= 0, so we clamp it to 0
        v1.a = 1.0f
        count = 1
        return
      }

      // w2 region
      val d12_1 = Vec2.dot(w2, e12)
      if (d12_1 <= 0.0f) {
        // a1 <= 0, so we clamp it to 0
        v2.a = 1.0f
        count = 1
        v1.set(v2)
        return
      }

      // Must be in e12 region.
      val inv_d12 = 1.0f / (d12_1 + d12_2)
      v1.a = d12_1 * inv_d12
      v2.a = d12_2 * inv_d12
      count = 2
    }

    /**
     * Solve a line segment using barycentric coordinates.<br></br> Possible regions:<br></br>
     * - points[2]<br></br>
     * - edge points[0]-points[2]<br></br>
     * - edge points[1]-points[2]<br></br>
     * - inside the triangle
     */
    fun solve3() {
      w1.set(v1.w)
      w2.set(v2.w)
      w3.set(v3.w)

      // Edge12
      // [1 1 ][a1] = [1]
      // [w1.e12 w2.e12][a2] = [0]
      // a3 = 0
      e12.set(w2).subLocal(w1)
      val w1e12 = Vec2.dot(w1, e12)
      val w2e12 = Vec2.dot(w2, e12)
      val d12_2 = -w1e12

      // Edge13
      // [1 1 ][a1] = [1]
      // [w1.e13 w3.e13][a3] = [0]
      // a2 = 0
      e13.set(w3).subLocal(w1)
      val w1e13 = Vec2.dot(w1, e13)
      val w3e13 = Vec2.dot(w3, e13)
      val d13_2 = -w1e13

      // Edge23
      // [1 1 ][a2] = [1]
      // [w2.e23 w3.e23][a3] = [0]
      // a1 = 0
      e23.set(w3).subLocal(w2)
      val w2e23 = Vec2.dot(w2, e23)
      val w3e23 = Vec2.dot(w3, e23)
      val d23_2 = -w2e23

      // Triangle123
      val n123 = Vec2.cross(e12, e13)
      val d123_1 = n123 * Vec2.cross(w2, w3)
      val d123_2 = n123 * Vec2.cross(w3, w1)
      val d123_3 = n123 * Vec2.cross(w1, w2)

      // w1 region
      if (d12_2 <= 0.0f && d13_2 <= 0.0f) {
        v1.a = 1.0f
        count = 1
        return
      }

      // e12
      if (w2e12 > 0.0f && d12_2 > 0.0f && d123_3 <= 0.0f) {
        val inv_d12 = 1.0f / (w2e12 + d12_2)
        v1.a = w2e12 * inv_d12
        v2.a = d12_2 * inv_d12
        count = 2
        return
      }

      // e13
      if (w3e13 > 0.0f && d13_2 > 0.0f && d123_2 <= 0.0f) {
        val inv_d13 = 1.0f / (w3e13 + d13_2)
        v1.a = w3e13 * inv_d13
        v3.a = d13_2 * inv_d13
        count = 2
        v2.set(v3)
        return
      }

      // w2 region
      if (w2e12 <= 0.0f && d23_2 <= 0.0f) {
        v2.a = 1.0f
        count = 1
        v1.set(v2)
        return
      }

      // w3 region
      if (w3e13 <= 0.0f && w3e23 <= 0.0f) {
        v3.a = 1.0f
        count = 1
        v1.set(v3)
        return
      }

      // e23
      if (w3e23 > 0.0f && d23_2 > 0.0f && d123_1 <= 0.0f) {
        val inv_d23 = 1.0f / (w3e23 + d23_2)
        v2.a = w3e23 * inv_d23
        v3.a = d23_2 * inv_d23
        count = 2
        v1.set(v3)
        return
      }

      // Must be in triangle123
      val inv_d123 = 1.0f / (d123_1 + d123_2 + d123_3)
      v1.a = d123_1 * inv_d123
      v2.a = d123_2 * inv_d123
      v3.a = d123_3 * inv_d123
      count = 3
    }
  }

  /**
   * A distance proxy is used by the GJK algorithm. It encapsulates any shape. TODO: see if we can
   * just do assignments with m_vertices, instead of copying stuff over
   *
   * @author daniel
   */
  class DistanceProxy {
    val vertices: Array<Vec2> = Array<Vec2>(Settings.MAX_POLYGON_VERTICES) { Vec2() }

    var count: Int = 0
    var radius: Float = 0f
    val buffer: Array<Vec2?> = arrayOfNulls(2)

    /**
     * Initialize the proxy using the given shape. The shape must remain in scope while the proxy is
     * in use.
     */
    fun set(shape: Shape, index: Int) {
      when (shape.m_type) {
        ShapeType.CIRCLE -> {
          val circle = shape as CircleShape
          vertices[0].set(circle.p)
          count = 1
          radius = circle.radius
        }
        ShapeType.POLYGON -> {
          val poly = shape as PolygonShape
          count = poly.count
          radius = poly.radius
          for (i in 0 until count) {
            vertices[i].set(poly.vertices[i])
          }
        }
        ShapeType.CHAIN -> {
          val chain = shape as ChainShape
          // assert is not supported in KMP.
          // assert(0 <= index && index < chain.m_count)
          buffer[0] = chain.vertices!![index]
          if (index + 1 < chain.count) {
            buffer[1] = chain.vertices!![index + 1]
          } else {
            buffer[1] = chain.vertices!![0]
          }
          vertices[0].set(buffer[0]!!)
          vertices[1].set(buffer[1]!!)
          count = 2
          radius = chain.radius
        }
        ShapeType.EDGE -> {
          val edge = shape as EdgeShape
          vertices[0].set(edge.vertex1)
          vertices[1].set(edge.vertex2)
          count = 2
          radius = edge.radius
        }
      }
    }

    /**
     * Get the supporting vertex index in the given direction.
     *
     * @param d
     * @return
     */
    fun getSupport(d: Vec2): Int {
      var bestIndex = 0
      var bestValue = Vec2.dot(vertices[0], d)
      for (i in 1 until count) {
        val value = Vec2.dot(vertices[i], d)
        if (value > bestValue) {
          bestIndex = i
          bestValue = value
        }
      }
      return bestIndex
    }

    /**
     * Get the supporting vertex in the given direction.
     *
     * @param d
     * @return
     */
    fun getSupportVertex(d: Vec2): Vec2 {
      var bestIndex = 0
      var bestValue = Vec2.dot(vertices[0], d)
      for (i in 1 until count) {
        val value = Vec2.dot(vertices[i], d)
        if (value > bestValue) {
          bestIndex = i
          bestValue = value
        }
      }
      return vertices[bestIndex]
    }
  }

  /**
   * Compute the closest points between two shapes. Supports any combination of: CircleShape and
   * PolygonShape. The simplex cache is input/output. On the first call set SimplexCache.count to
   * zero.
   *
   * @param output
   * @param cache
   * @param input
   */
  fun distance(output: DistanceOutput, cache: SimplexCache, input: DistanceInput) {
    GJK_CALLS++
    val proxyA = input.proxyA
    val proxyB = input.proxyB
    val transformA = input.transformA
    val transformB = input.transformB

    // Initialize the simplex.
    simplex.readCache(cache, proxyA, transformA, proxyB, transformB)

    // Get simplex vertices as an array.
    val vertices = simplex.vertices

    // These store the vertices of the last simplex so that we
    // can check for duplicates and prevent cycling.
    // (pooled above)
    simplex.getClosestPoint(closestPoint)
    var distanceSqr1 = closestPoint.lengthSquared()

    // Main iteration loop
    var iter = 0
    while (iter < GJK_MAX_ITERS) {

      // Copy simplex so we can identify duplicates.
      var saveCount = simplex.count
      for (i in 0 until saveCount) {
        saveA[i] = vertices[i].indexA
        saveB[i] = vertices[i].indexB
      }
      when (simplex.count) {
        1 -> {}
        2 -> simplex.solve2()
        3 -> simplex.solve3()
        else -> {
          // assert is not supported in KMP.
          // assert(false)
        }
      }

      // If we have 3 points, then the origin is in the corresponding triangle.
      if (simplex.count == 3) {
        break
      }

      // Compute closest point.
      simplex.getClosestPoint(closestPoint)
      var distanceSqr2 = closestPoint.lengthSquared()

      // ensure progress
      if (distanceSqr2 >= distanceSqr1) {
        // break;
      }
      distanceSqr1 = distanceSqr2

      // get search direction;
      simplex.getSearchDirection(d)

      // Ensure the search direction is numerically fit.
      if (d.lengthSquared() < Settings.EPSILON * Settings.EPSILON) {
        // The origin is probably contained by a line segment
        // or triangle. Thus the shapes are overlapped.

        // We can't return zero here even though there may be overlap.
        // In case the simplex is a point, segment, or triangle it is difficult
        // to determine if the origin is contained in the CSO or very close to it.
        break
      }
      /*
       * SimplexVertex* vertex = vertices + simplex.m_count; vertex.indexA =
       * proxyA.GetSupport(MulT(transformA.R, -d)); vertex.wA = Mul(transformA,
       * proxyA.GetVertex(vertex.indexA)); Vec2 wBLocal; vertex.indexB =
       * proxyB.GetSupport(MulT(transformB.R, d)); vertex.wB = Mul(transformB,
       * proxyB.GetVertex(vertex.indexB)); vertex.w = vertex.wB - vertex.wA;
       */

      // Compute a tentative new simplex vertex using support points.
      val vertex = vertices[simplex.count]
      Rot.mulTransUnsafe(transformA.q, d.negateLocal(), temp)
      vertex.indexA = proxyA.getSupport(temp)
      Transform.mulToOutUnsafe(transformA, proxyA.vertices[vertex.indexA], vertex.wA)
      // Vec2 wBLocal;
      Rot.mulTransUnsafe(transformB.q, d.negateLocal(), temp)
      vertex.indexB = proxyB.getSupport(temp)
      Transform.mulToOutUnsafe(transformB, proxyB.vertices[vertex.indexB], vertex.wB)
      vertex.w.set(vertex.wB).subLocal(vertex.wA)

      // Iteration count is equated to the number of support point calls.
      ++iter
      ++GJK_ITERS

      // Check for duplicate support points. This is the main termination criteria.
      var duplicate = false
      for (i in 0 until saveCount) {
        if (vertex.indexA == saveA[i] && vertex.indexB == saveB[i]) {
          duplicate = true
          break
        }
      }

      // If we found a duplicate support point we must exit to avoid cycling.
      if (duplicate) {
        break
      }

      // New vertex is ok and needed.
      ++simplex.count
    }
    GJK_MAX_ITERS = MathUtils.max(GJK_MAX_ITERS, iter)

    // Prepare output.
    simplex.getWitnessPoints(output.pointA, output.pointB)
    output.distance = MathUtils.distance(output.pointA, output.pointB)
    output.iterations = iter

    // Cache the simplex.
    simplex.writeCache(cache)

    // Apply radii if requested.
    if (input.useRadii) {
      val rA = proxyA.radius
      val rB = proxyB.radius
      if (output.distance > rA + rB && output.distance > Settings.EPSILON) {
        // Shapes are still no overlapped.
        // Move the witness points to the outer surface.
        output.distance -= rA + rB
        normal.set(output.pointB).subLocal(output.pointA)
        normal.normalize()
        temp.set(normal).mulLocal(rA)
        output.pointA.addLocal(temp)
        temp.set(normal).mulLocal(rB)
        output.pointB.subLocal(temp)
      } else {
        // Shapes are overlapped when radii are considered.
        // Move the witness points to the middle.
        // Vec2 p = 0.5f * (output.pointA + output.pointB);
        output.pointA.addLocal(output.pointB).mulLocal(.5f)
        output.pointB.set(output.pointA)
        output.distance = 0.0f
      }
    }
  }

  companion object {
    var GJK_CALLS = 0
    var GJK_ITERS = 0
    var GJK_MAX_ITERS = 20
  }
}
