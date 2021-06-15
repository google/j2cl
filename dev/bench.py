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

  targets = repo_util.get_benchmarks(argv.bench_name[0].replace("/", ":"))
  subprocess.run(["blaze", "build"] + list(targets.values()), check=True)
  for platform, target in targets.items():
    binary = "blaze-bin/" + target.replace(":", "/")
    subprocess.run(
        "echo -n '%s: ' && %s" % (platform, binary), shell=True, check=True)


def add_arguments(parser):
  parser.add_argument(
      "bench_name", nargs=1, metavar="<name>", help="benchmark name")
