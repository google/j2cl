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

class TaskControlBlock implements Task {

  /** The task is running and is currently scheduled. */
  static final int STATE_RUNNING = 0;
  /** The task has packets left to process. */
  static final int STATE_RUNNABLE = 1;
  /**
   * The task is not currently running. The task is not blocked as such and may be started by the
   * scheduler.
   */
  static final int STATE_SUSPENDED = 2;
  /** The task is blocked and cannot be run until it is explicitly released. */
  static final int STATE_HELD = 4;

  static final int STATE_SUSPENDED_RUNNABLE = STATE_SUSPENDED | STATE_RUNNABLE;
  static final int STATE_NOT_HELD = ~STATE_HELD;

  TaskControlBlock link;
  int id;
  int priority;
  private Packet queue;
  private final Task task;
  private int state;

  /** A task control block manages a task and the queue of work packages associated with it. */
  TaskControlBlock(TaskControlBlock link, int id, int priority, Packet queue, Task task) {
    this.link = link;
    this.id = id;
    this.priority = priority;
    this.queue = queue;
    this.task = task;
    if (queue == null) {
      this.state = STATE_SUSPENDED;
    } else {
      this.state = STATE_SUSPENDED_RUNNABLE;
    }
  }

  void setRunning() {
    this.state = STATE_RUNNING;
  }

  void markAsNotHeld() {
    this.state = this.state & STATE_NOT_HELD;
  }

  void markAsHeld() {
    this.state = this.state | STATE_HELD;
  }

  boolean isHeldOrSuspended() {
    return (this.state & STATE_HELD) != 0 || (this.state == STATE_SUSPENDED);
  }

  void markAsSuspended() {
    this.state = this.state | STATE_SUSPENDED;
  }

  void markAsRunnable() {
    this.state = this.state | STATE_RUNNABLE;
  }

  /** Runs this task, if it is ready to be run, and returns the next task to run. */
  TaskControlBlock run() {
    Packet packet;
    if (this.state == STATE_SUSPENDED_RUNNABLE) {
      packet = this.queue;
      this.queue = packet.link;
      if (this.queue == null) {
        this.state = STATE_RUNNING;
      } else {
        this.state = STATE_RUNNABLE;
      }
    } else {
      packet = null;
    }
    return this.task.run(packet);
  }

  @Override
  public TaskControlBlock run(Packet packet) {
    return run();
  }

  /**
   * Adds a packet to the worklist of this block's task, marks this as runnable if necessary, and
   * returns the next runnable object to run (the one with the highest priority).
   */
  TaskControlBlock checkPriorityAdd(TaskControlBlock task, Packet packet) {
    if (this.queue == null) {
      this.queue = packet;
      this.markAsRunnable();
      if (this.priority > task.priority) {
        return this;
      }
    } else {
      this.queue = packet.addTo(this.queue);
    }
    return task;
  }

  @Override
  public String toString() {
    return "tcb { " + this.task + "@" + this.state + " }";
  }
}
