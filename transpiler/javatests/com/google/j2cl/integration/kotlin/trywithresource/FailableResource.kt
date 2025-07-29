/*
 * Copyright 2023 Google Inc.
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
package trywithresource

/**
 * A helper class that emulates a resource that can be told to fail at the initialization stage or
 * close stage.
 */
internal class FailableResource(
  val name: String,
  val orderLog: MutableList<String>,
  val failureMode: FailureMode
) : AutoCloseable {
  enum class FailureMode {
    None,
    OnConstruction,
    OnClose
  }

  init {
    if (failureMode == FailureMode.OnConstruction) {
      orderLog.add(name + " throw OnConstruction")
      throw RuntimeException("OnConstruction")
    }
    orderLog.add(name + " OnConstruction")
  }

  override fun close() {
    if (failureMode == FailureMode.OnClose) {
      orderLog.add(name + " throw OnClose")
      throw RuntimeException("OnClose")
    }
    orderLog.add(name + " OnClose")
  }
}
