# Copyright 2021 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Runs a benchmark."""

import subprocess
import sys

import repo_util


def main(argv):
  if subprocess.call("v8 -e ''", shell=True):
    print("Make sure d8 is installed via jsvu")
    sys.exit(1)

  benchs = [
      (n, repo_util.get_benchmarks(n, argv.platforms)) for n in argv.bench_names
  ]

  print("Building...")
  # Join all the targets to build them in one shot.
  targets = sum([list(bench.values()) for (_, bench) in benchs], [])
  repo_util.run_cmd(["blaze", "build"] + targets)

  print("Starting benchmarks.")
  for name, bench_set in benchs:
    print("[%s]" % name)
    for platform, target in bench_set.items():
      print(platform, end=": ", flush=True)
      print(repo_util.run_cmd(["blaze-bin/" + target]), end="")


def add_arguments(parser):
  parser.add_argument(
      "bench_names", nargs="+", metavar="<name>", help="benchmark name(s)")
