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
"""Runs various tests in the repository."""

import argparse
import subprocess
import repo_util


def main(argv):
  root = "//"
  cmd = ["blaze", "test", "--keep_going"]
  cmd += [root + t + "/..." for t in argv.test_pattern] or [root + "..."]
  cmd += repo_util.create_test_filter(argv.platforms)
  subprocess.call(cmd)


def add_arguments(parser):
  parser.add_argument(
      "test_pattern",
      metavar="<root>",
      nargs="*",
      help="test root(s). e.g. transpiler jre")


def run_for_presubmit(argv):
  argv = argparse.Namespace(test_pattern=[], platforms=argv.platforms)
  main(argv)
