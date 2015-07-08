#!/usr/bin/python2.7
#
# Copyright 2015 Google Inc. All Rights Reserved.

"""Diffs readable optimized integration test JS with current CL changes."""


import sys


import process_util
import repo_util


# pylint: disable=global-variable-not-assigned
MANAGED_GOOGLE3_PATH = repo_util.MANAGED_GOOGLE3_PATH
MIN_DIFF_CL = 96781479


def main():
  global MANAGED_GOOGLE3_PATH
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
  repo_util.build_readable_optimized_test(test_name, cwd=MANAGED_GOOGLE3_PATH)
  process_util.run_cmd_get_output(
      ["clang-format", "-i",
       MANAGED_GOOGLE3_PATH + "/" + js_file_path])

  print ("  blaze building readable opt JS for '%s' in the live repo" %
         test_name)
  repo_util.build_readable_optimized_test(test_name)
  process_util.run_cmd_get_output(["clang-format", "-i", js_file_path])

  print "  starting meld"
  process_util.run_cmd_get_output(
      ["meld",
       MANAGED_GOOGLE3_PATH + "/" + js_file_path,
       js_file_path])

  print "  done"


main()
