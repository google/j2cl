#!/usr/bin/python2.7

# Copyright 2017 Google Inc.
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

"""Util funcs for running Blaze on int. tests in live  and j2size repo."""

import getpass
import os
import shutil
import sys
import process_util

J2CL_ROOT = "third_party/java_src/j2cl/"
J2CL_MINIFER = (
    J2CL_ROOT + "tools/java/com/google/j2cl/tools/minifier:J2clMinifier")
INTEGRATION_ROOT = (
    J2CL_ROOT + "transpiler/javatests/com/google/j2cl/transpiler/integration/")
TEST_TARGET_PATTERN = INTEGRATION_ROOT + "...:all"
OBFUSCATED_OPT_TEST_PATTERN = INTEGRATION_ROOT + "%s:optimized_js"
READABLE_OPT_TEST_PATTERN = INTEGRATION_ROOT + "%s:readable_optimized_js"
READABLE_TEST_PATTERN = INTEGRATION_ROOT + "%s:readable_unoptimized_js"


def build_tests(test_targets, cwd=None):
  """Blaze builds all integration tests in parallel."""
  process_util.run_cmd_get_output(
      ["blaze", "build"] + test_targets,
      cwd=cwd)


def get_obfuscated_optimized_test(test_name):
  """Returns the path to the obfuscated opt JS file the given test."""
  return OBFUSCATED_OPT_TEST_PATTERN % test_name


def get_readable_optimized_test(test_name):
  """Returns the path to the readable opt JS file the given test."""
  return READABLE_OPT_TEST_PATTERN % test_name


def get_readable_unoptimized_test(test_name):
  """Returns the path to the readable unoptimized JS file the given test."""
  return READABLE_TEST_PATTERN % test_name


def get_optimized_test(test_name):
  return OBFUSCATED_OPT_TEST_PATTERN % test_name


def get_all_optimized_tests(cwd=None):
  test_targets = process_util.run_cmd_get_output(
      [
          "blaze", "query",
          "filter('%s', %s)" %
          (OBFUSCATED_OPT_TEST_PATTERN % ".*", TEST_TARGET_PATTERN)
      ],
      cwd=cwd).split("\n")
  return filter(bool, test_targets)


def get_js_files_by_test_name(test_targets, uncompiled):
  """Finds and returns a test_name<->optimized_js_file map."""

  # Convert to a map of names<->jsFile pairs
  test_names = [
      process_util.extract_pattern(OBFUSCATED_OPT_TEST_PATTERN % "(.*?)",
                                   size_target) for size_target in test_targets
  ]
  extension = "-bundle.js" if uncompiled else ".js"
  js_files = [get_file_from_target(t, extension) for t in test_targets]
  return dict(zip(test_names, js_files))


def get_minimized_size(js_file):
  """Runs the given file through J2clMinifier."""
  minified = process_util.run_cmd_get_output(
      ["blaze", "run", J2CL_MINIFER, "--", os.path.abspath(js_file)])
  return len(minified)


def get_file_from_target(target, extension=".js"):
  return "blaze-bin/" + target.replace(":", "/") + extension


def sync_j2size_repo():
  process_util.run_cmd_get_output(
      [INTEGRATION_ROOT + "sync_j2size_repo.sh"])


def get_j2size_repo_path():
  return "/google/src/cloud/%s/j2cl-size/google3" % getpass.getuser()


def diff_target(get_target):
  """Diffs j2size repo and current CL for the output."""

  if len(sys.argv) != 2:
    print "must pass the name of the test to diff as an argument"
    return

  test_name = sys.argv[1]
  js_target = get_target(test_name)
  js_file_path = get_file_from_target(js_target)

  print "Constructing a diff of JS changes in '%s'." % test_name

  print ("  blaze building JS for '%s' in the j2size repo" %
         test_name)
  sync_j2size_repo()
  build_tests([js_target], get_j2size_repo_path())

  print "    formatting JS"
  orig_js_file = "/tmp/orig.%s.js" % test_name
  shutil.copyfile(get_j2size_repo_path() + "/" + js_file_path, orig_js_file)
  process_util.run_cmd_get_output(["clang-format", "-i", orig_js_file])

  print ("  blaze building JS for '%s' in the live repo" %
         test_name)
  build_tests([js_target])

  print "    formatting JS"
  modified_js_file = "/tmp/modified.%s.js" % test_name
  shutil.copyfile(js_file_path, modified_js_file)
  process_util.run_cmd_get_output(["clang-format", "-i", modified_js_file])

  print "  starting meld"
  process_util.run_cmd_get_output(["meld", orig_js_file, modified_js_file])

  print "  done"
