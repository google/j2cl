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

class WorkerTask implements Task {

  private final Scheduler scheduler;
  private int v1;
  private int v2;

  /** A task that manipulates work packets. */
  WorkerTask(Scheduler scheduler, int v1, int v2) {
    this.scheduler = scheduler;
    this.v1 = v1;
    this.v2 = v2;
  }

  @Override
  public TaskControlBlock run(Packet packet) {
    if (packet == null) {
      return this.scheduler.suspendCurrent();
    } else {
      if (this.v1 == Scheduler.ID_HANDLER_A) {
        this.v1 = Scheduler.ID_HANDLER_B;
      } else {
        this.v1 = Scheduler.ID_HANDLER_A;
      }
      packet.id = this.v1;
      packet.a1 = 0;
      for (int i = 0; i < Packet.DATA_SIZE; i++) {
        this.v2++;
        if (this.v2 > 26) {
          this.v2 = 1;
        }
        packet.a2[i] = this.v2;
      }
      return this.scheduler.queue(packet);
    }
  }

  @Override
  public String toString() {
    return "WorkerTask";
  }
}
