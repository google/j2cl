/*
 * Copyright 2017 Google Inc.
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
package box2d;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

public class Box2d {
  private static final int NUM_STEPS = 100;
  private static final int NUM_BALLS = 20;

  private final World world;

  private Box2d() {
    // Define the gravity vector.
    Vec2 gravity = new Vec2(0f, -9.8f);

    // Initialise the World.
    world = new World(gravity);

    // Create the ground (Something for dynamic bodies to collide with).
    {
      BodyDef groundBodyDef = new BodyDef();
      groundBodyDef.position.set(10, -40);
      groundBodyDef.type = BodyType.STATIC;

      // Create the Body in the World.
      Body ground = world.createBody(groundBodyDef);

      // Create the fixtures (physical aspects) of the ground body.
      FixtureDef groundEdgeFixtureDef = new FixtureDef();
      groundEdgeFixtureDef.density = 1.0f;
      groundEdgeFixtureDef.friction = 1.0f;
      groundEdgeFixtureDef.restitution = 0.4f;

      PolygonShape groundEdge = new PolygonShape();
      groundEdgeFixtureDef.shape = groundEdge;

      // Bottom Edge.
      groundEdge.setAsBox(100, 1);
      ground.createFixture(groundEdgeFixtureDef);
    }

    for (int i = 0; i < NUM_BALLS; i++) {
      spawnBall();
    }
  }

  private void spawnBall() {
    float dx = (float) Math.random() * 10f;
    float dy = (float) Math.random() * 10f - 5f;
    // Create a Ball.
    BodyDef ballBodyDef = new BodyDef();
    ballBodyDef.type = BodyType.DYNAMIC;
    ballBodyDef.position.set(5 + dx, 5 + dy); // Centre of the ground box.

    // Create the body for the ball within the World.
    Body ball = world.createBody(ballBodyDef);

    float radius = 0.5f + (float) Math.random();

    // Create the actual fixture representing the box.
    CircleShape ballShape = new CircleShape();
    ballShape.m_radius = radius; // Diameter of 1m.
    // ballShape.m_p is the offset relative to body. Default of (0,0)

    FixtureDef ballFixtureDef = new FixtureDef();
    ballFixtureDef.density = 1.0f; // Must have a density or else it won't
    // be affected by gravity.
    ballFixtureDef.restitution = 0.4f; // Define how bouncy the ball is.
    ballFixtureDef.friction = 0.2f;
    ballFixtureDef.shape = ballShape;

    // Add the fixture to the ball body.
    ball.createFixture(ballFixtureDef);
  }

  private void step(float secondsPerFrame) {
    world.step(secondsPerFrame, 8, 3);
  }

  public static void start() {
    Box2d m = new Box2d();
    for (int i = 0; i < NUM_STEPS; i++) {
      m.step(1);
    }
  }
}
