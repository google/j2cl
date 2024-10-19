/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.benchmarking.framework;

/** A Benchmark that is used for testing. */
public class MockBenchmark extends AbstractBenchmark {

  private final MockClock clock;
  private int counter = 0;
  boolean firstTime = true;
  private boolean runCalled;
  private boolean setupOneTimeCalled;
  private boolean setupCalled;
  private boolean tearDownCalled;
  private boolean tearDownOneTimeCalled;

  public MockBenchmark(MockClock clock) {
    this.clock = clock;
  }

  @Override
  public void setupOneTime() {
    if (setupOneTimeCalled) {
      throw new IllegalStateException("setupOneTime called more than once");
    }
    setupOneTimeCalled = true;
  }

  @Override
  public void setup() {
    if (!setupOneTimeCalled) {
      throw new IllegalStateException("setupOneTime not called before setup");
    }
    if (setupCalled) {
      throw new IllegalStateException("setup called more than once");
    }
    setupCalled = true;

    if (firstTime) {
      firstTime = false;
    } else {
      if (!tearDownCalled) {
        throw new IllegalStateException("tear down was not called before next setup");
      }
    }
  }

  @Override
  public Object run() {
    if (!setupCalled) {
      throw new IllegalStateException("setup not called before bench");
    }
    setupCalled = false;
    clock.advance();
    runCalled = true;
    return counter++;
  }

  @Override
  public void tearDown() {
    if (!runCalled) {
      throw new IllegalStateException("run not called before tearDown");
    }
    runCalled = false;
    tearDownCalled = true;
  }

  @Override
  public void tearDownOneTime() {
    tearDownOneTimeCalled = true;
  }

  // Visible for testing
  public boolean isTearDownOneTimeCalled() {
    return tearDownOneTimeCalled;
  }
}
