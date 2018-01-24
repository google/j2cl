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

"""Util funcs for running Blaze on int. tests in managed and unmanaged repo."""

import getpass
import os
from os.path import expanduser


import shutil
import sys
import process_util

J2CL_ROOT = "//third_party/java_src/j2cl/"
J2CL_MINIFER = (
    J2CL_ROOT + "tools/java/com/google/j2cl/tools/minifier:J2clMinifier")
INTEGRATION_ROOT = (
    J2CL_ROOT + "transpiler/javatests/com/google/j2cl/transpiler/integration/")
TEST_TARGET_PATTERN = INTEGRATION_ROOT + "...:all"
OBFUSCATED_OPT_TEST_PATTERN = INTEGRATION_ROOT + "%s:optimized_js"
READABLE_OPT_TEST_PATTERN = INTEGRATION_ROOT + "%s:readable_optimized_js"
READABLE_TEST_PATTERN = INTEGRATION_ROOT + "%s:readable_unoptimized_js"

HOME_DIR_PATH = expanduser("~")

GIT_MANAGED_REPO_PATH = HOME_DIR_PATH + "/.j2cl-size-repo"
GIT_GOOGLE3_PATH = GIT_MANAGED_REPO_PATH + "/google3"

CITC_GOOGLE3_PATH = ("/google/src/cloud/%s/j2cl-size/google3" %
                     getpass.getuser())


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


def compute_synced_to_cl():
  """Returns the cl that version control is currently synced to."""
  if is_git():
    status_line = process_util.run_cmd_get_output(["git5", "status"])
    synced_to_cl = process_util.extract_pattern("Synced at CL (.*?) = ",
                                                status_line)
    return int(synced_to_cl)
  else:
    synced_to_cl = process_util.run_cmd_get_output(["srcfs", "get_readonly"])
    return int(synced_to_cl)


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
  return target.replace("//", "blaze-bin/").replace(":", "/") + extension


def managed_repo_sync_to(cl):
  if is_git():
    process_util.run_cmd_get_output(
        ["git5", "sync", "@" + str(cl), "--rebase"],
        cwd=GIT_GOOGLE3_PATH)
  else:
    process_util.run_cmd_get_output(
        ["g4", "sync", "@" + str(cl)],
        cwd=CITC_GOOGLE3_PATH)


def managed_repo_validate_environment():
  """Ensure expected directories exist."""
  if is_git():
    if not os.path.isdir(GIT_MANAGED_REPO_PATH):
      print("  Creating managed opt size tracking git5 repo at '%s'" %
            GIT_MANAGED_REPO_PATH)
      os.mkdir(GIT_MANAGED_REPO_PATH)
      process_util.run_cmd_get_output(
          ["git5", "start", "base",
           "//depot/google3/third_party/java_src/j2cl"],
          cwd=GIT_MANAGED_REPO_PATH)
  else:
    if not os.path.isdir(CITC_GOOGLE3_PATH):
      print("  Creating managed opt size tracking citc client at '%s'" %
            CITC_GOOGLE3_PATH)
      process_util.run_cmd_get_output(["g4", "citc", "j2cl-size"])


def check_out_file(file_name):
  """Edits/adds the specified file in the current VCS, if necessary."""
  if is_git():
    # No action necessary if in a git client.
    return
  if not os.path.isfile(file_name):
    process_util.run_cmd_get_output(["g4", "add", file_name])
  else:
    process_util.run_cmd_get_output(["g4", "edit", file_name])


_IS_GIT = None


def is_git():
  """Returns whether this is a git client."""
  global _IS_GIT
  if _IS_GIT is None:
    _IS_GIT = True
    try:
      process_util.run_cmd_get_output(["git", "branch"])
    except process_util.CmdExecutionError:
      _IS_GIT = False
  return _IS_GIT


def get_cwd(managed):
  return None if not managed else get_managed_path()


def get_managed_path():
  return GIT_GOOGLE3_PATH if is_git() else CITC_GOOGLE3_PATH


def diff_target(get_target):
  """Diffs managed repo and current CL for the output."""

  if len(sys.argv) != 2:
    print "must pass the name of the test to diff as an argument"
    return

  test_name = sys.argv[1]
  js_target = get_target(test_name)
  js_file_path = get_file_from_target(js_target)

  print "Constructing a diff of obfuscated opt JS changes in '%s'." % test_name

  managed_repo_validate_environment()

  synced_to_cl = compute_synced_to_cl()

  print "  syncing managed repo to your same sync CL " + str(synced_to_cl)
  managed_repo_sync_to(synced_to_cl)

  print ("  blaze building obfuscated opt JS for '%s' in the managed repo" %
         test_name)
  build_tests([js_target], get_managed_path())

  print "    formatting JS"
  managed_js_file = "/tmp/managed.%s.js" % test_name
  shutil.copyfile(get_managed_path() + "/" + js_file_path,
                  managed_js_file)
  process_util.run_cmd_get_output(["clang-format", "-i", managed_js_file])

  print ("  blaze building obfuscated opt JS for '%s' in the live repo" %
         test_name)
  build_tests([js_target])

  print "    formatting JS"
  live_js_file = "/tmp/live.%s.js" % test_name
  shutil.copyfile(js_file_path, live_js_file)
  process_util.run_cmd_get_output(["clang-format", "-i", live_js_file])

  print "  starting meld"
  process_util.run_cmd_get_output(["meld", managed_js_file, live_js_file])

  print "  done"
