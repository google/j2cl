/*
 * Copyright 2023 Google Inc.
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
package box2d

import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World

class Box2d private constructor() {
  private val world: World

  init {
    // Define the gravity vector.
    val gravity = Vec2(0f, -9.8f)

    // Initialise the World.
    world = World(gravity)

    // Create the ground (Something for dynamic bodies to collide with).
    val groundBodyDef = BodyDef()
    groundBodyDef.position.set(10f, -40f)
    groundBodyDef.type = BodyType.STATIC

    // Create the Body in the World.
    val ground = world.createBody(groundBodyDef)!!

    // Create the fixtures (physical aspects) of the ground body.
    val groundEdgeFixtureDef = FixtureDef()
    groundEdgeFixtureDef.density = 1.0f
    groundEdgeFixtureDef.friction = 1.0f
    groundEdgeFixtureDef.restitution = 0.4f

    val groundEdge = PolygonShape()
    groundEdgeFixtureDef.shape = groundEdge

    // Bottom Edge.
    groundEdge.setAsBox(100f, 1f)
    ground.createFixture(groundEdgeFixtureDef)

    for (i in 0 until NUM_BALLS) {
      spawnBall()
    }
  }

  private fun spawnBall() {
    val dx = Math.random().toFloat() * 10f
    val dy = Math.random().toFloat() * 10f - 5f
    // Create a Ball.
    val ballBodyDef = BodyDef()
    ballBodyDef.type = BodyType.DYNAMIC
    ballBodyDef.position.set(5 + dx, 5 + dy) // Centre of the ground box.

    // Create the body for the ball within the World.
    val ball = world.createBody(ballBodyDef)!!

    val radius = 0.5f + Math.random().toFloat()

    // Create the actual fixture representing the box.
    val ballShape = CircleShape()
    ballShape.m_radius = radius // Diameter of 1m.
    // ballShape.m_p is the offset relative to body. Default of (0,0)

    val ballFixtureDef = FixtureDef()
    ballFixtureDef.density = 1.0f // Must have a density or else it won't
    // be affected by gravity.
    ballFixtureDef.restitution = 0.4f // Define how bouncy the ball is.
    ballFixtureDef.friction = 0.2f
    ballFixtureDef.shape = ballShape

    // Add the fixture to the ball body.
    ball.createFixture(ballFixtureDef)
  }

  private fun step(secondsPerFrame: Float) {
    world.step(secondsPerFrame, 8, 3)
  }

  companion object {
    private const val NUM_STEPS = 100
    private const val NUM_BALLS = 20

    fun start() {
      val m = Box2d()
      for (i in 0 until NUM_STEPS) {
        m.step(1f)
      }
    }
  }
}
