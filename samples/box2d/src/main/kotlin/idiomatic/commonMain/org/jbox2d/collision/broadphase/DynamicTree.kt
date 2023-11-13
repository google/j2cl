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
package org.jbox2d.collision.broadphase

import org.jbox2d.callbacks.DebugDraw
import org.jbox2d.callbacks.TreeCallback
import org.jbox2d.callbacks.TreeRayCastCallback
import org.jbox2d.collision.AABB
import org.jbox2d.collision.RayCastInput
import org.jbox2d.common.Color3f
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2

/**
 * A dynamic tree arranges data in a binary tree to accelerate queries such as volume queries and
 * ray casts. Leafs are proxies with an AABB. In the tree we expand the proxy AABB by _fatAABBFactor
 * so that the proxy AABB is bigger than the client object. This allows the client object to move by
 * small amounts without triggering a tree update.
 *
 * @author daniel
 */
class DynamicTree : BroadPhaseStrategy {
  private var root: DynamicTreeNode? = null
  private var nodeCount = 0
  private var nodeCapacity = 16
  private var freeList: Int = 0
  private var insertionCount: Int = 0
  private val drawVecs = Array(4) { Vec2() }
  private val nodeStack = TreeNodeStack(10)
  private var treeNodes: Array<DynamicTreeNode> =
    Array(16) { DynamicTreeNode(it) }
      .apply {
        for (i in nodeCapacity - 1 downTo 0) {
          this[i].parent = if (i == nodeCapacity - 1) null else this[i + 1]
          this[i].height = -1
        }
      }
  private val r = Vec2()
  private val aabb = AABB()
  private val subInput = RayCastInput()
  private val combinedAABB = AABB()
  private val color = Color3f()
  private val textVec = Vec2()

  override fun createProxy(aabb: AABB, userData: Any): Int {
    val node = allocateNode()
    val proxyId = node.id
    // Fatten the aabb
    val nodeAABB = node.aabb
    nodeAABB.lowerBound.x = aabb.lowerBound.x - Settings.AABB_EXTENSION
    nodeAABB.lowerBound.y = aabb.lowerBound.y - Settings.AABB_EXTENSION
    nodeAABB.upperBound.x = aabb.upperBound.x + Settings.AABB_EXTENSION
    nodeAABB.upperBound.y = aabb.upperBound.y + Settings.AABB_EXTENSION
    node.userData = userData
    insertLeaf(proxyId)
    return proxyId
  }

  override fun destroyProxy(proxyId: Int) {
    // assert is not supported in KMP.
    // assert(0 <= proxyId && proxyId < nodeCapacity)
    val node = treeNodes[proxyId]
    // assert is not supported in KMP.
    // assert(node.isLeaf())
    removeLeaf(node)
    freeNode(node)
  }

  override fun moveProxy(proxyId: Int, aabb: AABB, displacement: Vec2): Boolean {
    // assert is not supported in KMP.
    // assert(0 <= proxyId && proxyId < nodeCapacity)
    val node = treeNodes[proxyId]
    // assert is not supported in KMP.
    // assert(node.isLeaf())
    val nodeAABB = node.aabb
    // if (nodeAABB.contains(aabb)) {
    if (
      nodeAABB.lowerBound.x > aabb.lowerBound.x &&
        nodeAABB.lowerBound.y > aabb.lowerBound.y &&
        aabb.upperBound.x > nodeAABB.upperBound.x &&
        aabb.upperBound.y > nodeAABB.upperBound.y
    ) {
      return false
    }
    removeLeaf(node)

    // Extend AABB
    val lowerBound = nodeAABB.lowerBound
    val upperBound = nodeAABB.upperBound
    lowerBound.x = aabb.lowerBound.x - Settings.AABB_EXTENSION
    lowerBound.y = aabb.lowerBound.y - Settings.AABB_EXTENSION
    upperBound.x = aabb.upperBound.x + Settings.AABB_EXTENSION
    upperBound.y = aabb.upperBound.y + Settings.AABB_EXTENSION

    // Predict AABB displacement.
    val dx = displacement.x * Settings.AABB_MULTIPLIER
    val dy = displacement.y * Settings.AABB_MULTIPLIER
    if (dx < 0.0f) {
      lowerBound.x += dx
    } else {
      upperBound.x += dx
    }
    if (dy < 0.0f) {
      lowerBound.y += dy
    } else {
      upperBound.y += dy
    }
    insertLeaf(proxyId)
    return true
  }

  override fun getUserData(proxyId: Int): Any = treeNodes[proxyId].userData!!

  override fun getFatAABB(proxyId: Int): AABB = treeNodes[proxyId].aabb

  override fun query(callback: TreeCallback, aabb: AABB) {
    nodeStack.reset()
    nodeStack.push(root!!)
    while (nodeStack.getCount() > 0) {
      val node = nodeStack.pop() ?: continue
      if (AABB.testOverlap(node.aabb, aabb)) {
        if (node.child1 == null) {
          val proceed = callback.treeCallback(node.id)
          if (!proceed) {
            return
          }
        } else {
          nodeStack.push(node.child1!!)
          nodeStack.push(node.child2!!)
        }
      }
    }
  }

  override fun raycast(callback: TreeRayCastCallback, input: RayCastInput) {
    val p1 = input.p1
    val p2 = input.p2
    val p1x = p1.x
    val p2x = p2.x
    val p1y = p1.y
    val p2y = p2.y
    val vx: Float
    val vy: Float
    var cx: Float
    var cy: Float
    var hx: Float
    var hy: Float
    var tempx: Float
    var tempy: Float
    r.x = p2x - p1x
    r.y = p2y - p1y
    // assert is not supported in KMP.
    // assert(r.x * r.x + r.y * r.y > 0f)
    r.normalize()
    val rx: Float = r.x
    val ry: Float = r.y

    // v is perpendicular to the segment.
    vx = -1f * ry
    vy = 1f * rx
    val absVx: Float = MathUtils.abs(vx)
    val absVy: Float = MathUtils.abs(vy)

    // Separating axis for segment (Gino, p80).
    // |dot(v, p1 - c)| > dot(|v|, h)
    var maxFraction = input.maxFraction

    // Build a bounding box for the segment.
    val segAABB = aabb
    // Vec2 t = p1 + maxFraction * (p2 - p1);
    // before inline
    // temp.set(p2).subLocal(p1).mulLocal(maxFraction).addLocal(p1);
    // Vec2.minToOut(p1, temp, segAABB.lowerBound);
    // Vec2.maxToOut(p1, temp, segAABB.upperBound);
    tempx = (p2x - p1x) * maxFraction + p1x
    tempy = (p2y - p1y) * maxFraction + p1y
    segAABB.lowerBound.x = if (p1x < tempx) p1x else tempx
    segAABB.lowerBound.y = if (p1y < tempy) p1y else tempy
    segAABB.upperBound.x = if (p1x > tempx) p1x else tempx
    segAABB.upperBound.y = if (p1y > tempy) p1y else tempy
    // end inline
    nodeStack.reset()
    nodeStack.push(root!!)
    while (nodeStack.getCount() > 0) {
      val node = nodeStack.pop() ?: continue
      val nodeAABB = node.aabb
      if (!AABB.testOverlap(nodeAABB, segAABB)) {
        continue
      }

      // Separating axis for segment (Gino, p80).
      // |dot(v, p1 - c)| > dot(|v|, h)
      // node.aabb.getCenterToOut(c);
      // node.aabb.getExtentsToOut(h);
      cx = (nodeAABB.lowerBound.x + nodeAABB.upperBound.x) * .5f
      cy = (nodeAABB.lowerBound.y + nodeAABB.upperBound.y) * .5f
      hx = (nodeAABB.upperBound.x - nodeAABB.lowerBound.x) * .5f
      hy = (nodeAABB.upperBound.y - nodeAABB.lowerBound.y) * .5f
      tempx = p1x - cx
      tempy = p1y - cy
      val separation = MathUtils.abs(vx * tempx + vy * tempy) - (absVx * hx + absVy * hy)
      if (separation > 0.0f) {
        continue
      }
      if (node.isLeaf()) {
        subInput.p1.x = p1x
        subInput.p1.y = p1y
        subInput.p2.x = p2x
        subInput.p2.y = p2y
        subInput.maxFraction = maxFraction
        val value = callback.raycastCallback(subInput, node.id)
        if (value == 0.0f) {
          // The client has terminated the ray cast.
          return
        }
        if (value > 0.0f) {
          // Update segment bounding box.
          maxFraction = value
          // temp.set(p2).subLocal(p1).mulLocal(maxFraction).addLocal(p1);
          // Vec2.minToOut(p1, temp, segAABB.lowerBound);
          // Vec2.maxToOut(p1, temp, segAABB.upperBound);
          tempx = (p2x - p1x) * maxFraction + p1x
          tempy = (p2y - p1y) * maxFraction + p1y
          segAABB.lowerBound.x = if (p1x < tempx) p1x else tempx
          segAABB.lowerBound.y = if (p1y < tempy) p1y else tempy
          segAABB.upperBound.x = if (p1x > tempx) p1x else tempx
          segAABB.upperBound.y = if (p1y > tempy) p1y else tempy
        }
      } else {
        nodeStack.push(node.child1!!)
        nodeStack.push(node.child2!!)
      }
    }
  }

  override fun computeHeight(): Int = computeHeight(root!!)

  private fun computeHeight(node: DynamicTreeNode): Int {
    // assert is not supported in KMP.
    // assert(0 <= node!!.id && node.id < nodeCapacity)
    if (node.isLeaf()) {
      return 0
    }
    val height1 = computeHeight(node.child1!!)
    val height2 = computeHeight(node.child2!!)
    return 1 + MathUtils.max(height1, height2)
  }

  /** Validate this tree. For testing. */
  fun validate() {
    validateStructure(root)
    validateMetrics(root)
    var freeCount = 0
    var freeNode = if (freeList != NULL_NODE) treeNodes[freeList] else null
    while (freeNode != null) {
      // assert is not supported in KMP.
      // assert(0 <= freeNode.id && freeNode.id < nodeCapacity)
      // assert(freeNode === treeNodes[freeNode.id])
      freeNode = freeNode.parent
      ++freeCount
    }
    // assert is not supported in KMP.
    // assert(getHeight() == computeHeight())
    // assert(nodeCount + freeCount == nodeCapacity)
  }

  override fun getHeight(): Int {
    if (root == null) {
      return 0
    }
    return root!!.height
  }

  override fun getMaxBalance(): Int {
    var maxBalance = 0
    for (i in 0 until nodeCapacity) {
      val node = treeNodes[i]
      if (node.height <= 1) {
        continue
      }
      // assert is not supported in KMP.
      // assert(node.isLeaf() == false)
      val child1 = node.child1
      val child2 = node.child2
      val balance = MathUtils.abs(child2!!.height - child1!!.height)
      maxBalance = MathUtils.max(maxBalance, balance)
    }
    return maxBalance
  }

  // Free node in pool
  override fun getAreaRatio(): Float {
    if (root == null) {
      return 0.0f
    }
    val rootNode: DynamicTreeNode = root!!
    val rootArea = rootNode.aabb.perimeter
    var totalArea = 0.0f
    for (i in 0 until nodeCapacity) {
      val node = treeNodes[i]
      if (node.height < 0) {
        // Free node in pool
        continue
      }
      totalArea += node.aabb.perimeter
    }
    return totalArea / rootArea
  }

  /** Build an optimal tree. Very expensive. For testing. */
  fun rebuildBottomUp() {
    val nodes = IntArray(nodeCount)
    var count = 0

    // Build array of leaves. Free the rest.
    for (i in 0 until nodeCapacity) {
      if (treeNodes[i].height < 0) {
        // free node in pool
        continue
      }
      val node = treeNodes[i]
      if (node.isLeaf()) {
        node.parent = null
        nodes[count] = i
        ++count
      } else {
        freeNode(node)
      }
    }
    val b = AABB()
    while (count > 1) {
      var minCost = Float.MAX_VALUE
      var iMin = -1
      var jMin = -1
      for (i in 0 until count) {
        val aabbi = treeNodes[nodes[i]].aabb
        for (j in i + 1 until count) {
          val aabbj = treeNodes[nodes[j]].aabb
          b.combine(aabbi, aabbj)
          val cost: Float = b.perimeter
          if (cost < minCost) {
            iMin = i
            jMin = j
            minCost = cost
          }
        }
      }
      val index1 = nodes[iMin]
      val index2 = nodes[jMin]
      val child1 = treeNodes[index1]
      val child2 = treeNodes[index2]
      val parent = allocateNode()
      parent.child1 = child1
      parent.child2 = child2
      parent.height = 1 + MathUtils.max(child1.height, child2.height)
      parent.aabb.combine(child1.aabb, child2.aabb)
      parent.parent = null
      child1.parent = parent
      child2.parent = parent
      nodes[jMin] = nodes[count - 1]
      nodes[iMin] = parent.id
      --count
    }
    root = treeNodes[nodes[0]]
    validate()
  }

  private fun allocateNode(): DynamicTreeNode {
    if (freeList == NULL_NODE) {
      // assert is not supported in KMP.
      // assert(nodeCount == nodeCapacity)
      val old = treeNodes
      nodeCapacity *= 2
      treeNodes = Array(nodeCapacity) { DynamicTreeNode(it) }
      old.copyInto(treeNodes)

      // Build a linked list for the free list.
      for (i in nodeCapacity - 1 downTo nodeCount) {
        treeNodes[i].parent = if (i == nodeCapacity - 1) null else treeNodes[i + 1]
        treeNodes[i].height = -1
      }
      freeList = nodeCount
    }
    val nodeId = freeList
    val treeNode = treeNodes[nodeId]
    freeList = if (treeNode.parent != null) treeNode.parent!!.id else NULL_NODE
    treeNode.parent = null
    treeNode.child1 = null
    treeNode.child2 = null
    treeNode.height = 0
    treeNode.userData = null
    ++nodeCount
    return treeNode
  }

  /** returns a node to the pool */
  private fun freeNode(node: DynamicTreeNode) {
    // assert is not supported in KMP.
    // assert(node != null)
    // assert(0 < nodeCount)
    node.parent = if (freeList != NULL_NODE) treeNodes[freeList] else null
    node.height = -1
    freeList = node.id
    nodeCount--
  }

  override fun getInsertionCount(): Int = insertionCount

  private fun insertLeaf(leaf_index: Int) {
    insertionCount++
    val leaf = treeNodes[leaf_index]
    if (root == null) {
      root = leaf
      root!!.parent = null
      return
    }

    // find the best sibling
    val leafAABB = leaf.aabb
    var index = root
    while (index!!.child1 != null) {
      val node = index
      val child1 = node.child1
      val child2 = node.child2
      val area = node.aabb.perimeter
      combinedAABB.combine(node.aabb, leafAABB)
      val combinedArea: Float = combinedAABB.perimeter

      // Cost of creating a new parent for this node and the new leaf
      val cost = 2.0f * combinedArea

      // Minimum cost of pushing the leaf further down the tree
      val inheritanceCost = 2.0f * (combinedArea - area)

      // Cost of descending into child1
      combinedAABB.combine(leafAABB, child1!!.aabb)
      val cost1: Float =
        if (child1.isLeaf()) {
          combinedAABB.perimeter + inheritanceCost
        } else {
          val oldArea = child1.aabb.perimeter
          val newArea: Float = combinedAABB.perimeter
          newArea - oldArea + inheritanceCost
        }

      combinedAABB.combine(leafAABB, child2!!.aabb)
      // Cost of descending into child2
      val cost2: Float =
        if (child2!!.isLeaf()) {
          combinedAABB.perimeter + inheritanceCost
        } else {
          val oldArea = child2.aabb.perimeter
          val newArea: Float = combinedAABB.perimeter
          newArea - oldArea + inheritanceCost
        }

      // Descend according to the minimum cost.
      if (cost < cost1 && cost < cost2) {
        break
      }

      // Descend
      index =
        if (cost1 < cost2) {
          child1
        } else {
          child2
        }
    }
    val sibling = index
    val oldParent = treeNodes[sibling.id].parent
    val newParent = allocateNode()
    newParent.parent = oldParent
    newParent.userData = null
    newParent.aabb.combine(leafAABB, sibling.aabb)
    newParent.height = sibling.height + 1
    if (oldParent != null) {
      // The sibling was not the root.
      if (oldParent.child1 === sibling) {
        oldParent.child1 = newParent
      } else {
        oldParent.child2 = newParent
      }
      newParent.child1 = sibling
      newParent.child2 = leaf
      sibling.parent = newParent
      leaf.parent = newParent
    } else {
      // The sibling was the root.
      newParent.child1 = sibling
      newParent.child2 = leaf
      sibling.parent = newParent
      leaf.parent = newParent
      root = newParent
    }

    // Walk back up the tree fixing heights and AABBs
    index = leaf.parent
    while (index != null) {
      index = balance(index)
      val child1 = index.child1
      val child2 = index.child2
      // assert is not supported in KMP.
      // assert(child1 != null)
      // assert(child2 != null)
      index.height = 1 + MathUtils.max(child1!!.height, child2!!.height)
      index.aabb.combine(child1.aabb, child2.aabb)
      index = index.parent
    }

    // validate();
  }

  private fun removeLeaf(leaf: DynamicTreeNode) {
    if (leaf === root) {
      root = null
      return
    }
    val parent = leaf.parent
    val grandParent = parent!!.parent
    val sibling: DynamicTreeNode? =
      if (parent.child1 === leaf) {
        parent.child2
      } else {
        parent.child1
      }
    if (grandParent != null) {
      // Destroy parent and connect sibling to grandParent.
      if (grandParent.child1 === parent) {
        grandParent.child1 = sibling
      } else {
        grandParent.child2 = sibling
      }
      sibling!!.parent = grandParent
      freeNode(parent)

      // Adjust ancestor bounds.
      var index = grandParent
      while (index != null) {
        index = balance(index)
        val child1 = index.child1
        val child2 = index.child2
        index.aabb.combine(child1!!.aabb, child2!!.aabb)
        index.height = 1 + MathUtils.max(child1.height, child2.height)
        index = index.parent
      }
    } else {
      root = sibling
      sibling!!.parent = null
      freeNode(parent)
    }

    // validate();
  }

  // Perform a left or right rotation if node A is imbalanced.
  // Returns the new root index.
  private fun balance(iA: DynamicTreeNode): DynamicTreeNode {
    // assert is not supported in KMP.
    // assert(iA != null)
    if (iA.isLeaf() || iA.height < 2) {
      return iA
    }
    val iB = iA.child1
    val iC = iA.child2
    // assert is not supported in KMP.
    // assert(0 <= iB!!.id && iB.id < nodeCapacity)
    // assert(0 <= iC!!.id && iC.id < nodeCapacity)
    val balance = iC!!.height - iB!!.height

    // Rotate C up
    if (balance > 1) {
      val iF = iC.child1
      val iG = iC.child2

      // assert is not supported in KMP.
      // assert(iF != null)
      // assert(iG != null)
      // assert(0 <= iF!!.id && iF.id < nodeCapacity)
      // assert(0 <= iG!!.id && iG.id < nodeCapacity)

      // Swap A and C
      iC.child1 = iA
      iC.parent = iA.parent
      iA.parent = iC

      // A's old parent should point to C
      if (iC.parent != null) {
        if (iC.parent!!.child1 === iA) {
          iC.parent!!.child1 = iC
        } else {
          // assert is not supported in KMP.
          // assert(iC.parent!!.child2 === iA)
          iC.parent!!.child2 = iC
        }
      } else {
        root = iC
      }

      // Rotate
      if (iF!!.height > iG!!.height) {
        iC.child2 = iF
        iA.child2 = iG
        iG.parent = iA
        iA.aabb.combine(iB.aabb, iG.aabb)
        iC.aabb.combine(iA.aabb, iF.aabb)
        iA.height = 1 + MathUtils.max(iB.height, iG.height)
        iC.height = 1 + MathUtils.max(iA.height, iF.height)
      } else {
        iC.child2 = iG
        iA.child2 = iF
        iF.parent = iA
        iA.aabb.combine(iB.aabb, iF.aabb)
        iC.aabb.combine(iA.aabb, iG.aabb)
        iA.height = 1 + MathUtils.max(iB.height, iF.height)
        iC.height = 1 + MathUtils.max(iA.height, iG.height)
      }
      return iC
    }

    // Rotate B up
    if (balance < -1) {
      val iD = iB.child1
      val iE = iB.child2
      // assert is not supported in KMP.
      // assert(0 <= iD!!.id && iD.id < nodeCapacity)
      // assert(0 <= iE!!.id && iE.id < nodeCapacity)

      // Swap A and B
      iB.child1 = iA
      iB.parent = iA.parent
      iA.parent = iB

      // A's old parent should point to B
      if (iB.parent != null) {
        iB.parent!!.let {
          if (it.child1 === iA) {
            it.child1 = iB
          } else {
            // assert is not supported in KMP.
            // assert(iB.parent!!.child2 === iA)
            it.child2 = iB
          }
        }
      } else {
        root = iB
      }

      // Rotate
      if (iD!!.height > iE!!.height) {
        iB.child2 = iD
        iA.child1 = iE
        iE.parent = iA
        iA.aabb.combine(iC.aabb, iE.aabb)
        iB.aabb.combine(iA.aabb, iD.aabb)
        iA.height = 1 + MathUtils.max(iC.height, iE.height)
        iB.height = 1 + MathUtils.max(iA.height, iD.height)
      } else {
        iB.child2 = iE
        iA.child1 = iD
        iD.parent = iA
        iA.aabb.combine(iC.aabb, iD.aabb)
        iB.aabb.combine(iA.aabb, iE.aabb)
        iA.height = 1 + MathUtils.max(iC.height, iD.height)
        iB.height = 1 + MathUtils.max(iA.height, iE.height)
      }
      return iB
    }
    return iA
  }

  private fun validateStructure(node: DynamicTreeNode?) {
    if (node == null) {
      return
    }
    // assert is not supported in KMP.
    // assert(node === treeNodes[node.id])
    // if (node === root) {
    //  assert(node.parent == null)
    // }
    val child1 = node.child1
    val child2 = node.child2
    if (node.isLeaf()) {
      // assert is not supported in KMP.
      // assert(child1 == null)
      // assert(child2 == null)
      // assert(node.height == 0)
      return
    }

    // assert is not supported in KMP.
    // assert(child1 != null && 0 <= child1.id && child1.id < nodeCapacity)
    // assert(child2 != null && 0 <= child2.id && child2.id < nodeCapacity)
    // assert(child1!!.parent === node)
    // assert(child2!!.parent === node)
    validateStructure(child1)
    validateStructure(child2)
  }

  private fun validateMetrics(node: DynamicTreeNode?) {
    if (node == null) {
      return
    }
    val child1 = node.child1
    val child2 = node.child2
    if (node.isLeaf()) {
      // assert is not supported in KMP.
      // assert(child1 == null)
      // assert(child2 == null)
      // assert(node.height == 0)
      return
    }
    // assert is not supported in KMP.
    // assert(child1 != null && 0 <= child1.id && child1.id < nodeCapacity)
    // assert(child2 != null && 0 <= child2.id && child2.id < nodeCapacity)
    val height1 = child1!!.height
    val height2 = child2!!.height
    @Suppress("UNUSED_VARIABLE") val height: Int = 1 + MathUtils.max(height1, height2)
    // assert is not supported in KMP.
    // assert(node.height == height)
    val aabb = AABB()
    aabb.combine(child1.aabb, child2.aabb)
    // assert is not supported in KMP.
    // assert(aabb.lowerBound == node.aabb.lowerBound)
    // assert(aabb.upperBound == node.aabb.upperBound)
    validateMetrics(child1)
    validateMetrics(child2)
  }

  override fun drawTree(draw: DebugDraw) {
    if (root == null) {
      return
    }
    val height = computeHeight()
    drawTree(draw, root!!, 0, height)
  }

  fun drawTree(argDraw: DebugDraw, node: DynamicTreeNode, spot: Int, height: Int) {
    node.aabb.getVertices(drawVecs)
    color.set(1f, (height - spot) * 1f / height, (height - spot) * 1f / height)
    argDraw.drawPolygon(drawVecs, 4, color)
    argDraw.viewportTransform.getWorldToScreen(node.aabb.upperBound, textVec)
    argDraw.drawString(
      textVec.x,
      textVec.y,
      node.id.toString() + "-" + (spot + 1) + "/" + height,
      color
    )
    if (node.child1 != null) {
      drawTree(argDraw, node.child1!!, spot + 1, height)
    }
    if (node.child2 != null) {
      drawTree(argDraw, node.child2!!, spot + 1, height)
    }
  }

  inner class TreeNodeStack(private var size: Int) {
    private var stack: Array<DynamicTreeNode?> = arrayOfNulls(size)
    private var position: Int = 0

    fun reset() {
      position = 0
    }

    fun pop(): DynamicTreeNode? = stack[--position]

    fun push(i: DynamicTreeNode) {
      if (position == size) {
        val old = stack
        stack = arrayOfNulls<DynamicTreeNode>(size * 2)
        old.copyInto(stack)
        size = stack.size
      }
      stack[position++] = i
    }

    fun getCount(): Int = position
  }

  companion object {
    const val MAX_STACK_SIZE = 64
    const val NULL_NODE = -1
  }
}
