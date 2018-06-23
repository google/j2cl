/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler;

import com.google.j2cl.bazel.BazelWorker;

/**
 * Runs J2clTranspiler as a blaze worker.
 *
 * <p>Blaze workers allow compilers to run in a JVM that is not being terminated after every compile
 * and thus gain significant speedups. This class implements the blaze worker protocol and drives
 * J2clTranspiler as a worker.
 */
public class J2clTranspilerWorker {
  public static void main(String[] workerArgs) {
    BazelWorker.start(
        workerArgs, (args, output) -> J2clCommandLineRunner.run(args).reportAndGetExitCode(output));
  }
}
