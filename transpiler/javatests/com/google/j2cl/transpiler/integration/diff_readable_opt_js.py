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

"""Diffs readable optimized integration test JS with current CL changes."""


import shutil
import sys


import process_util
import repo_util


# pylint: disable=global-variable-not-assigned
MIN_DIFF_CL = 96781479


def main():
  global MIN_DIFF_CL

  if len(sys.argv) != 2:
    print "must pass the name of the test to diff as an argument"
    return
  test_name = sys.argv[1]
  js_file_path = repo_util.get_readable_optimized_test_file(test_name)

  print "Constructing a diff of opt JS changes in '%s'." % test_name

  repo_util.managed_repo_validate_environment()

  synced_to_cl = repo_util.compute_synced_to_cl()
  if synced_to_cl < MIN_DIFF_CL:
    print(
        "  must be synced to atleast CL %s to be able to do readable diff." %
        MIN_DIFF_CL)
    return

  print "  syncing managed repo to your same sync CL " + str(synced_to_cl)
  repo_util.managed_repo_sync_to(synced_to_cl)

  print ("  blaze building readable opt JS for '%s' in the managed repo" %
         test_name)
  repo_util.build_readable_optimized_test(test_name,
                                          repo_util.get_managed_path())

  print "    formatting JS"
  managed_js_file = "/tmp/managed.%s.js" % test_name
  shutil.copyfile(repo_util.get_managed_path() + "/" + js_file_path,
                  managed_js_file)
  process_util.run_cmd_get_output(["clang-format", "-i", managed_js_file])

  print ("  blaze building readable opt JS for '%s' in the live repo" %
         test_name)
  repo_util.build_readable_optimized_test(test_name)

  print "    formatting JS"
  live_js_file = "/tmp/live.%s.js" % test_name
  shutil.copyfile(js_file_path, live_js_file)
  process_util.run_cmd_get_output(["clang-format", "-i", live_js_file])

  print "  starting meld"
  process_util.run_cmd_get_output(["meld", managed_js_file, live_js_file])

  print "  done"


main()
