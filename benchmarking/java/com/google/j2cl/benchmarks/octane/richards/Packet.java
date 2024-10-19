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

class Packet {

  static final int DATA_SIZE = 4;

  Packet link;
  int id;
  int a1;
  int[] a2;
  int kind;

  /**
   * A simple package of data that is manipulated by the tasks. The exact layout of the payload data
   * carried by a packet is not importaint, and neither is the nature of the work performed on
   * packets by the tasks.
   *
   * <p>Besides carrying data, packets form linked lists and are hence used both as data and
   * worklists.
   */
  Packet(Packet link, int id, int kind) {
    this.link = link;
    this.id = id;
    this.kind = kind;
    this.a1 = 0;
    this.a2 = new int[DATA_SIZE];
  }

  /** Add this packet to the end of a worklist, and return the worklist. */
  Packet addTo(Packet queue) {
    this.link = null;
    if (queue == null) {
      return this;
    }
    Packet peek;
    Packet next = queue;
    while ((peek = next.link) != null) {
      next = peek;
    }
    next.link = this;
    return queue;
  }

  @Override
  public String toString() {
    return "Packet";
  }
}
