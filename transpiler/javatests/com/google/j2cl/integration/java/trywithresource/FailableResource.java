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
package trywithresource;

import java.util.List;

/**
 * A helper class that emulates a resource that can be told to fail at the initialization stage
 * or close stage.
 */
class FailableResource implements AutoCloseable {
  public enum FailureMode {
    None,
    OnConstruction,
    OnClose
  }

  List<String> orderLog;
  String name;
  FailureMode failureMode;

  public FailableResource(String name, List<String> orderLog, FailureMode failureMode) {
    this.orderLog = orderLog;
    this.name = name;
    this.failureMode = failureMode;
    if (this.failureMode == FailureMode.OnConstruction) {
      this.orderLog.add(this.name + " throw OnConstruction");
      throw new RuntimeException("OnConstruction");
    }
    this.orderLog.add(this.name + " OnConstruction");
  }

  @Override
  public void close() {
    if (failureMode == FailureMode.OnClose) {
      orderLog.add(this.name + " throw OnClose");
      throw new RuntimeException("OnClose");
    }
    orderLog.add(this.name + " OnClose");
  }
}
