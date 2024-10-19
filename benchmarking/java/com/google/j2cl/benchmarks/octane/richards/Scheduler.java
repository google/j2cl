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

import javax.annotation.Nullable;

/**
 * A scheduler can be used to schedule a set of tasks based on their relative priorities. Scheduling
 * is done by maintaining a list of task control blocks which holds tasks and the data queue they
 * are processing.
 */
class Scheduler {

  static final int ID_IDLE = 0;
  static final int ID_WORKER = 1;
  static final int ID_HANDLER_A = 2;
  static final int ID_HANDLER_B = 3;
  static final int ID_DEVICE_A = 4;
  static final int ID_DEVICE_B = 5;
  static final int NUMBER_OF_IDS = 6;
  static final int KIND_DEVICE = 0;
  static final int KIND_WORK = 1;

  int queueCount;
  int holdCount;
  private TaskControlBlock list;
  private TaskControlBlock currentTcb;
  private int currentId;
  private final TaskControlBlock[] blocks;

  Scheduler() {
    this.queueCount = 0;
    this.holdCount = 0;
    this.blocks = new TaskControlBlock[NUMBER_OF_IDS];
    this.list = null;
    this.currentTcb = null;
    this.currentId = -1;
  }

  /** Add an idle task to this scheduler. */
  void addIdleTask(int id, int priority, Packet queue, int count) {
    this.addRunningTask(id, priority, queue, new IdleTask(this, 1, count));
  }

  /** Add a work task to this scheduler. */
  void addWorkerTask(int id, int priority, Packet queue) {
    this.addTask(id, priority, queue, new WorkerTask(this, ID_HANDLER_A, 0));
  }

  /** Add a handler task to this scheduler. */
  void addHandlerTask(int id, int priority, Packet queue) {
    this.addTask(id, priority, queue, new HandlerTask(this));
  }

  /** Add a handler task to this scheduler. */
  void addDeviceTask(int id, int priority, Packet queue) {
    this.addTask(id, priority, queue, new DeviceTask(this));
  }

  /** Add the specified task and mark it as running. */
  void addRunningTask(int id, int priority, Packet queue, Task task) {
    this.addTask(id, priority, queue, task);
    this.currentTcb.setRunning();
  }

  /** Add the specified task to this scheduler. */
  private void addTask(int id, int priority, Packet queue, Task task) {
    this.currentTcb = new TaskControlBlock(this.list, id, priority, queue, task);
    this.list = this.currentTcb;
    this.blocks[id] = this.currentTcb;
  }

  /** Execute the tasks managed by this scheduler. */
  void schedule() {
    this.currentTcb = this.list;
    while (this.currentTcb != null) {
      if (this.currentTcb.isHeldOrSuspended()) {
        this.currentTcb = this.currentTcb.link;
      } else {
        this.currentId = this.currentTcb.id;
        this.currentTcb = this.currentTcb.run();
      }
    }
  }

  /** Release a task that is currently blocked and return the next block to run. */
  @Nullable
  TaskControlBlock release(int id) {
    TaskControlBlock tcb = this.blocks[id];
    if (tcb == null) {
      return tcb;
    }
    tcb.markAsNotHeld();
    if (tcb.priority > this.currentTcb.priority) {
      return tcb;
    } else {
      return this.currentTcb;
    }
  }

  /**
   * Block the currently executing task and return the next task control block to run. The blocked
   * task will not be made runnable until it is explicitly released, even if new work is added to
   * it.
   */
  TaskControlBlock holdCurrent() {
    this.holdCount++;
    this.currentTcb.markAsHeld();
    return this.currentTcb.link;
  }

  /**
   * Suspend the currently executing task and return the next task control block to run. If new work
   * is added to the suspended task it will be made runnable.
   */
  TaskControlBlock suspendCurrent() {
    this.currentTcb.markAsSuspended();
    return this.currentTcb;
  }

  /**
   * Add the specified packet to the end of the worklist used by the task associated with the packet
   * and make the task runnable if it is currently suspended.
   */
  @Nullable
  TaskControlBlock queue(Packet packet) {
    TaskControlBlock t = this.blocks[packet.id];
    if (t == null) {
      return t;
    }
    this.queueCount++;
    packet.link = null;
    packet.id = this.currentId;
    return t.checkPriorityAdd(this.currentTcb, packet);
  }
}
