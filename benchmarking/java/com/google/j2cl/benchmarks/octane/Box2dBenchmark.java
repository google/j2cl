/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.benchmarks.octane;

import com.google.j2cl.benchmarking.framework.AbstractBenchmark;
import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

/**
 * Port of the V8 Octane Box2d Benchmark: https://github.com/chromium/octane/blob/master/box2d.js
 */
public class Box2dBenchmark extends AbstractBenchmark {
  private static final int NUMBER_OF_STEPS = 20;

  private World makeNewWorld() {
    Vec2 gravity = new Vec2(0, -10);
    World world = new World(gravity);
    EdgeShape edgeShape = new EdgeShape();
    edgeShape.set(new Vec2(-40.0f, 0), new Vec2(40.0f, 0));

    FixtureDef fd = new FixtureDef();
    fd.density = 0.0f;
    fd.shape = edgeShape;
    BodyDef bd = new BodyDef();
    Body ground = world.createBody(bd);
    ground.createFixture(fd);

    float a = .5f;
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(a, a);

    Vec2 x = new Vec2(-7.0f, 0.75f);
    Vec2 y = new Vec2();
    Vec2 deltaX = new Vec2(0.5625f, 1);
    Vec2 deltaY = new Vec2(1.125f, 0.0f);

    for (int i = 0; i < 10; ++i) {
      y.set(x.x, x.y);

      for (int j = 0; j < 5; ++j) {
        fd = new FixtureDef();
        fd.density = 5.0f;
        fd.shape = shape;

        bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(y.x, y.y);
        Body body = world.createBody(bd);
        body.createFixture(fd);
        y = y.add(deltaY);
      }

      x = x.add(deltaX);
    }

    return world;
  }

  @Override
  public Object run() {
    World world = makeNewWorld();
    for (int i = 0; i < NUMBER_OF_STEPS; i++) {
      world.step(1 / 60f, 10, 3);
    }
    return world;
  }
}
