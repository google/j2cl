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

class HandlerTask implements Task {

  private final Scheduler scheduler;
  private Packet v1;
  private Packet v2;

  /** A task that manipulates work packets and then suspends itself. */
  HandlerTask(Scheduler scheduler) {
    this.scheduler = scheduler;
    this.v1 = null;
    this.v2 = null;
  }

  @Override
  public TaskControlBlock run(Packet packet) {
    if (packet != null) {
      if (packet.kind == Scheduler.KIND_WORK) {
        this.v1 = packet.addTo(this.v1);
      } else {
        this.v2 = packet.addTo(this.v2);
      }
    }
    if (this.v1 != null) {
      int count = this.v1.a1;
      Packet v;
      if (count < Packet.DATA_SIZE) {
        if (this.v2 != null) {
          v = this.v2;
          this.v2 = this.v2.link;
          v.a1 = this.v1.a2[count];
          this.v1.a1 = count + 1;
          return this.scheduler.queue(v);
        }
      } else {
        v = this.v1;
        this.v1 = this.v1.link;
        return this.scheduler.queue(v);
      }
    }
    return this.scheduler.suspendCurrent();
  }

  @Override
  public String toString() {
    return "HandlerTask";
  }
}
