/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.tools.gwtincompatible;

import com.google.j2cl.bazel.BazelWorker;

/** Runs The @GwtIncompatible stripper as a worker. */
public class StripperWorker {
  public static void main(String[] workerArgs) {
    BazelWorker.start(
        workerArgs, (args, output) -> Stripper.strip(args).reportAndGetExitCode(output));
  }
}
