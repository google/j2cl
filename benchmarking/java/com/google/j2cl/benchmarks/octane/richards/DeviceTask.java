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
package com.google.j2cl.benchmarks.octane.richards;

class DeviceTask implements Task {

  private final Scheduler scheduler;
  private Packet v1;

  /**
   * A task that suspends itself after each time it has been run to simulate waiting for data from
   * an external device.
   */
  DeviceTask(Scheduler scheduler) {
    this.scheduler = scheduler;
    this.v1 = null;
  }

  @Override
  public TaskControlBlock run(Packet packet) {
    if (packet == null) {
      if (this.v1 == null) {
        return this.scheduler.suspendCurrent();
      }
      Packet v = this.v1;
      this.v1 = null;
      return this.scheduler.queue(v);
    } else {
      this.v1 = packet;
      return this.scheduler.holdCurrent();
    }
  }

  @Override
  public String toString() {
    return "DeviceTask";
  }
}
