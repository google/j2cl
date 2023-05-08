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
"""Diffs optimized integration test JS with current CL changes."""

import argparse
import shutil
import subprocess

import repo_util


def main(argv):
  test_name = argv.test_name[0]

  print("Constructing a diff of JS changes in '%s'." % test_name)

  repo_util.sync_j2size_repo()

  js_target = repo_util.get_readable_optimized_test(test_name)
  js_file_path = "blaze-bin/" + repo_util.get_file_from_target(js_target)

  print("  blaze building JS for '%s'" % test_name)
  repo_util.build_original_and_modified([js_target], [js_target])

  print("  Formatting")
  orig_js_file = "/tmp/orig.%s.js" % test_name.replace("/", ".")
  shutil.copyfile(repo_util.get_j2size_repo_path() + "/" + js_file_path,
                  orig_js_file)
  modified_js_file = "/tmp/modified.%s.js" % test_name.replace("/", ".")
  shutil.copyfile(js_file_path, modified_js_file)
  repo_util.run_cmd(["clang-format", "-i", orig_js_file, modified_js_file])

  if argv.filter_noise:
    print("  Reducing noise")
    # Replace the numeric part of the variable id generation from JsCompiler to
    # reduce noise in the final diff.
    # The patterns we want to match are:
    #   $jscomp$1234
    #   $jscomp$inline_1234
    #   JSC$1234
    # The numeric part of these patterns will be replaced by the character '#'
    repo_util.run_cmd([
        "sed",
        "-i",
        "-E",
        r"s/(\$jscomp\$(inline_)?|JSC\$)[0-9]+/\1#/g",
        orig_js_file,
        modified_js_file,
    ])

  print("  Starting diff")
  subprocess.call(
      "${P4DIFF:-diff} %s %s" % (orig_js_file, modified_js_file), shell=True)


def add_arguments(parser):
  parser.add_argument(
      "--filter_noise",
      default=True,
      action=argparse.BooleanOptionalAction,
      help="Filter noise in the diff due to difference in variable indexes.",
  )
  parser.add_argument(
      "test_name", nargs=1, metavar="<name>", help="integration test name")
