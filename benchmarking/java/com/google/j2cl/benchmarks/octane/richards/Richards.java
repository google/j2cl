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

/** This is the Java port of http://www.cl.cam.ac.uk/~mr10/Bench.html */
public class Richards {

  public static final int COUNT = 1000;

  /**
   * Following two constants specify how many times a packet is queued and how many times a task is
   * put on hold in a correct run of richards. They don't have any meaning a such but are
   * characteristic of a correct run so if the actual queue or hold count is different from the
   * expected there must be a bug in the implementation.
   */
  public static final int EXPECTED_QUEUE_COUNT = 2322;

  public static final int EXPECTED_HOLD_COUNT = 928;

  /** The Richards benchmark simulates the task dispatcher of an operating system. */
  public void runRichards() {
    Scheduler scheduler = new Scheduler();
    scheduler.addIdleTask(Scheduler.ID_IDLE, 0, null, COUNT);

    Packet queue = new Packet(null, Scheduler.ID_WORKER, Scheduler.KIND_WORK);
    queue = new Packet(queue, Scheduler.ID_WORKER, Scheduler.KIND_WORK);
    scheduler.addWorkerTask(Scheduler.ID_WORKER, 1000, queue);

    queue = new Packet(null, Scheduler.ID_DEVICE_A, Scheduler.KIND_DEVICE);
    queue = new Packet(queue, Scheduler.ID_DEVICE_A, Scheduler.KIND_DEVICE);
    queue = new Packet(queue, Scheduler.ID_DEVICE_A, Scheduler.KIND_DEVICE);
    scheduler.addHandlerTask(Scheduler.ID_HANDLER_A, 2000, queue);

    queue = new Packet(null, Scheduler.ID_DEVICE_B, Scheduler.KIND_DEVICE);
    queue = new Packet(queue, Scheduler.ID_DEVICE_B, Scheduler.KIND_DEVICE);
    queue = new Packet(queue, Scheduler.ID_DEVICE_B, Scheduler.KIND_DEVICE);
    scheduler.addHandlerTask(Scheduler.ID_HANDLER_B, 3000, queue);

    scheduler.addDeviceTask(Scheduler.ID_DEVICE_A, 4000, null);

    scheduler.addDeviceTask(Scheduler.ID_DEVICE_B, 5000, null);

    scheduler.schedule();

    if (scheduler.queueCount != EXPECTED_QUEUE_COUNT
        || scheduler.holdCount != EXPECTED_HOLD_COUNT) {
      String msg =
          "Error during execution: queueCount = "
              + scheduler.queueCount
              + ", holdCount = "
              + scheduler.holdCount
              + ".";
      throw new AssertionError(msg);
    }
  }
}
