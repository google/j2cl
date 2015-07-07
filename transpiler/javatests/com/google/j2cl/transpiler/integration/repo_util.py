#!/usr/bin/python2.7
#
# Copyright 2015 Google Inc. All Rights Reserved.

"""Util funcs for running Blaze on int. tests in managed and unmanaged repo."""


import os
from os.path import expanduser


import process_util


# pylint: disable=global-variable-not-assigned
TEST_TARGET_PATTERN = ("third_party/java_src/j2cl/transpiler/javatests/"
                       "com/google/j2cl/transpiler/integration/...:all")
READABLE_TEST_PATTERN = ("//third_party/java_src/j2cl/transpiler/javatests/"
                         "com/google/j2cl/transpiler/integration/"
                         "%s:readable_optimized_js")
READABLE_TEST_FILE = (
    "blaze-bin/third_party/java_src/j2cl/transpiler/javatests/"
    "com/google/j2cl/transpiler/integration/"
    "%s/readable_optimized_js.js")
HOME_DIR_PATH = expanduser("~")
MANAGED_REPO_PATH = HOME_DIR_PATH + "/.j2cl-size-repo"
MANAGED_GOOGLE3_PATH = MANAGED_REPO_PATH + "/google3"
MANAGED_DATA_DIR_PATH = HOME_DIR_PATH + "/.j2cl-size-data"
MANAGED_DATA_LAST_CL_PATH = (
    MANAGED_DATA_DIR_PATH + "/last_optimized_size_cl.txt")


def build_optimized_tests(cwd=None):
  """Blaze builds all integration tests in parallel."""
  process_util.run_cmd_get_output(
      ["blaze", "build", TEST_TARGET_PATTERN], cwd=cwd)


def get_readable_optimized_test_file(test_name):
  """Returns the path to the readable opt JS file the given test."""
  global READABLE_TEST_FILE

  return READABLE_TEST_FILE % test_name


def build_readable_optimized_test(test_name, cwd=None):
  """Blaze builds the readable JS for a particular test."""
  global READABLE_TEST_PATTERN

  process_util.run_cmd_get_output(
      ["blaze", "build", READABLE_TEST_PATTERN % test_name], cwd=cwd)


def compute_synced_to_cl():
  """Returns the cl that git5 is currently synced to."""
  status_line = process_util.run_cmd_get_output(["git5", "status"])
  synced_to_cl = process_util.extract_pattern(
      "Synced at CL (.*?) = ", status_line)
  return int(synced_to_cl)


def get_js_files_by_test_name(cwd=None):
  """Finds and returns a test_name<->optimized_js_file map."""
  # Gather a list of the names of the test targets we care about
  test_targets = (
      process_util.run_cmd_get_output(
          ["blaze", "query",
           "filter('.*:optimized_js', kind(%s, %s))" %
           ("js_binary", TEST_TARGET_PATTERN)], cwd=cwd).split("\n"))
  test_targets = filter(bool, test_targets)

  # Convert to a map of names<->jsFile pairs
  test_names = [
      process_util.extract_pattern(
          ".*integration/(.*?):optimized_js", size_target)
      for size_target in test_targets]
  js_files = [
      size_target.replace("//", "blaze-bin/").replace(":", "/") + ".js"
      for size_target in test_targets]
  return zip(test_names, js_files)


def find_cls_in_since(in_dir, since_cl):
  """Returns committed changes since the given cl in the given dir."""
  return process_util.run_cmd_get_output(
      ["clsearch", "file:" + in_dir, "from:" + str(since_cl + 1)]).split("\n")


def find_key_cls_since(since_cl):
  """Returns important changes since the given cl."""
  unprocessed_cls = []
  unprocessed_cls.extend(
      find_cls_in_since("third_party/java_src/j2cl", since_cl))
  unprocessed_cls.extend(
      find_cls_in_since("third_party/java_src/jscomp", since_cl))
  unprocessed_cls.extend(
      find_cls_in_since("third_party/java/eclipse", since_cl))

  unprocessed_cls = sorted(
      set(
          [
              process_util.extract_pattern(
                  "http://cl/(.*?) on .*",
                  unprocessed_cl)
              for unprocessed_cl in unprocessed_cls if unprocessed_cl]))

  if unprocessed_cls:
    print("    There are changes (in j2cl/jscomp/jdt) to record for CLs: " +
          str(unprocessed_cls))

  return unprocessed_cls


def managed_repo_compute_test_size_path(test_name):
  """Returns the path to the size log for the given test."""
  global MANAGED_DATA_DIR_PATH

  return "%s/%s_optimized_size.dat" % (MANAGED_DATA_DIR_PATH, test_name)


def managed_repo_sync_to(cl):
  process_util.run_cmd_get_output(
      ["git5", "sync", "@" + str(cl), "--rebase"], cwd=MANAGED_GOOGLE3_PATH)


def managed_repo_process_cl(cl):
  """Record optimized test sizes at the given cl."""
  global MANAGED_DATA_DIR_PATH
  global MANAGED_DATA_LAST_CL_PATH
  global MANAGED_GOOGLE3_PATH

  print "    processing CL " + str(cl)
  managed_repo_sync_to(cl)

  try:
    print "      blaze building optimized tests"
    build_optimized_tests(MANAGED_GOOGLE3_PATH)

    optimized_size_change_count = 0
    # For every test that exists in the managed repo at its currently synced CL.
    js_files_by_test_name = get_js_files_by_test_name(cwd=MANAGED_GOOGLE3_PATH)
    for (test_name, js_file) in js_files_by_test_name:
      absolute_js_file_path = MANAGED_GOOGLE3_PATH + "/" + js_file
      if not os.path.isfile(absolute_js_file_path):
        # This test does not exist in the managed repo at the CL at which
        # it is synced. Skip it
        continue

      optimized_size = os.path.getsize(absolute_js_file_path)
      optimized_size_log_path = managed_repo_compute_test_size_path(test_name)

      # If the file does not exist then open in a mode that will create it.
      write_mode = "ra+" if os.path.isfile(optimized_size_log_path) else "w+"
      optimized_size_log_file = open(optimized_size_log_path, write_mode)

      if os.path.getsize(optimized_size_log_path) == 0:
        # Write the first entry.
        optimized_size_log_file.write("%s\t%s\n" % (cl, optimized_size))
        print("        '%s' first optimized size: %s bytes" %
              (test_name, optimized_size))
        optimized_size_change_count += 1
      else:
        # See if size has changed since the last recording.
        last_line = optimized_size_log_file.readlines()[-1]
        last_cl, last_optimized_size = last_line.split("\t")

        last_cl = int(last_cl)
        last_optimized_size = int(last_optimized_size)

        if last_optimized_size != optimized_size:
          # If size has changed, add a record.
          optimized_size_log_file.write("%s\t%s\n" % (cl, optimized_size))
          print("        '%s' updated optimized size: %s->%s bytes" %
                (test_name, last_optimized_size, optimized_size))
          optimized_size_change_count += 1
      optimized_size_log_file.close()

    print(
        "      there were %s optimized size changes" %
        optimized_size_change_count)
  except process_util.CmdExecutionError:
    print "Failed to blaze build @%s, skipping the unanalyzable CL." % str(cl)

  # Record progress
  last_processed_cl_file = open(MANAGED_DATA_LAST_CL_PATH, "w+")
  last_processed_cl_file.write(str(cl))
  last_processed_cl_file.close()


def managed_repo_get_last_processed_cl():
  """Read and return the last processed cl number from disk."""
  global MANAGED_DATA_LAST_CL_PATH

  last_processed_cl_file = open(MANAGED_DATA_LAST_CL_PATH, "r")
  last_optimized_size_cl = int(last_processed_cl_file.read())
  last_processed_cl_file.close()
  return last_optimized_size_cl


def managed_repo_validate_environment():
  """Ensure expected directories exist."""
  global MANAGED_REPO_PATH
  global MANAGED_DATA_DIR_PATH
  global MANAGED_DATA_LAST_CL_PATH

  if not os.path.isdir(MANAGED_REPO_PATH):
    print("  Creating managed opt size tracking git5 repo at '%s'" %
          MANAGED_REPO_PATH)
    os.mkdir(MANAGED_REPO_PATH)
    process_util.run_cmd_get_output(
        ["git5", "start", "base", "//depot/google3/third_party/java_src/j2cl"],
        cwd=MANAGED_REPO_PATH)

  if not os.path.isdir(MANAGED_DATA_DIR_PATH):
    print("  Creating managed opt size tracking data dir at '%s'" %
          MANAGED_DATA_DIR_PATH)
    os.mkdir(MANAGED_DATA_DIR_PATH)

  if not os.path.isfile(MANAGED_DATA_LAST_CL_PATH):
    first_cl = 97364419
    print "  Starting tracking at cl %s" % first_cl
    last_processed_cl_file = open(MANAGED_DATA_LAST_CL_PATH, "w+")
    last_processed_cl_file.write(str(first_cl))
    last_processed_cl_file.close()


def managed_repo_update_sizes_cache():
  """Caches optimized test sizes for newly seen committed cls."""
  print "  Refreshing the size cache (requires clsearch)."
  last_processed_cl = managed_repo_get_last_processed_cl()
  unprocessed_cls = find_key_cls_since(last_processed_cl)
  for unprocessed_cl in unprocessed_cls:
    managed_repo_process_cl(unprocessed_cl)
  print "  Size cache is fresh."
