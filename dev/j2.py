#!/usr/bin/python3

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
"""Entry point for various tools useful for J2CL development."""

import argparse
import sys

import bench
import diff
import make_size_report
import presubmit
import replace_all
import test_all
import test_integration


def _add_cmd(subparsers, name, handler, descr):
  parser = subparsers.add_parser(name, help=descr, description=descr)
  if hasattr(handler, "add_arguments"):
    handler.add_arguments(parser)
  parser.set_defaults(func=handler.main)


_PLATFORMS = ["CLOSURE", "WASM", "JVM", "J2KT"]

if __name__ == "__main__":
  base_parser = argparse.ArgumentParser(description="j2 dev script.")
  base_parser.add_argument(
      "-p",
      action="append",
      choices=_PLATFORMS,
      dest="platforms",
      help="Configure the platforms included for this command.")
  parsers = base_parser.add_subparsers()

  _add_cmd(parsers, "size", make_size_report, "Generate size report.")
  _add_cmd(parsers, "gen", replace_all, "Regenerate readable examples.")
  _add_cmd(parsers, "bench", bench, "Run a benchmark.")
  _add_cmd(parsers, "diff", diff, "Compare integration test output.")
  _add_cmd(parsers, "test", test_integration, "Run an integration test.")
  _add_cmd(parsers, "testall", test_all, "Run all tests.")
  _add_cmd(parsers, "presubmit", presubmit,
           "Run the tasks needed before submit")

  args = base_parser.parse_args()
  if not hasattr(args, "func"):
    base_parser.print_help(sys.stderr)
    sys.exit(1)
  args.platforms = args.platforms or _PLATFORMS
  args.func(args)
