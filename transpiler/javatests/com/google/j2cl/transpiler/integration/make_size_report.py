#!/usr/bin/python2.7
#
# Copyright 2015 Google Inc. All Rights Reserved.

"""Reports optimized size changes caused by the current CL."""


import getpass
import os
from os.path import expanduser
import re
from subprocess import PIPE
from subprocess import Popen
import time


# pylint: disable=global-variable-not-assigned
HOME_DIR_PATH = expanduser("~")
MANAGED_REPO_PATH = HOME_DIR_PATH + "/.j2cl-size-repo"
MANAGED_GOOGLE3_PATH = MANAGED_REPO_PATH + "/google3"
TEST_TARGET_PATTERN = ("third_party/java_src/j2cl/transpiler/javatests/"
                       "com/google/j2cl/transpiler/integration/...:all")
DATA_DIR_PATH = HOME_DIR_PATH + "/.j2cl-size-data"
LAST_OPT_SIZE_CL_PATH = DATA_DIR_PATH + "/last_optimized_size_cl.txt"
SUCCESS_CODE = 0


class BashInvocationError(Exception):
  """Indicates that a bash invocation returned a non-zero exit code."""


def build_optimized_tests(cwd=None):
  """Blaze builds all integration tests in parallel."""
  get_cmd_output(["blaze", "build", TEST_TARGET_PATTERN], cwd=cwd)


def compute_synced_to_cl():
  """Returns the cl that git5 is currently synced to."""
  status_line = get_cmd_output(["git5", "status"])
  synced_to_cl = extract_pattern("Synced at CL (.*) = ", status_line)
  return synced_to_cl


def extract_pattern(pattern_string, from_value):
  """Returns the regex matched value."""
  return re.compile(pattern_string).search(from_value).group(1)


def find_last_stats(stat_lines, synced_to_cl):
  """Returns the opt size in the highest CL before the current sync cl."""
  last_optimized_size = 0

  for stat_line in stat_lines:
    cl, optimized_size = stat_line.split("\t")
    cl = int(cl)
    optimized_size = int(optimized_size)

    if cl > synced_to_cl:
      break
    else:
      last_optimized_size = optimized_size

  return last_optimized_size


def find_cls_in_since(in_dir, since_cl):
  """Returns committed changes since the given cl in the given dir."""
  return get_cmd_output(
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
      set([extract_pattern("http://cl/(.*) on .*", unprocessed_cl) for
           unprocessed_cl in unprocessed_cls if unprocessed_cl]))

  if unprocessed_cls:
    print("    There are changes (in j2cl/jscomp/jdt) to record for CLs: " +
          str(unprocessed_cls))

  return unprocessed_cls


def get_cmd_output(bash_args, cwd=None):
  """Runs a bash command and returns output as a string."""
  global SUCCESS_CODE

  process = Popen(bash_args, stdin=PIPE, stdout=PIPE, stderr=PIPE, cwd=cwd)
  output = process.communicate()[0]
  if process.wait() != SUCCESS_CODE:
    raise BashInvocationError("bash invocation " + str(bash_args) + " FAILED")

  return output


def get_js_files_by_test_name():
  """Finds and returns a test_name<->optimized_js_file map."""
  # Gather a list of the names of the test targets we care about
  test_targets = (
      get_cmd_output(
          ["blaze", "query",
           "filter('.*optimized_js', kind(%s, %s))" %
           ("js_binary", TEST_TARGET_PATTERN)]).split("\n"))
  test_targets = filter(bool, test_targets)

  # Convert to a map of names<->jsFile pairs
  test_names = [
      extract_pattern(".*integration/(.*):optimized_js", size_target)
      for size_target in test_targets]
  js_files = [
      size_target.replace("//", "blaze-bin/").replace(":", "/") + ".js"
      for size_target in test_targets]
  return zip(test_names, js_files)


def get_last_processed_cl():
  """Read and return the last processed cl number from disk."""
  global LAST_OPT_SIZE_CL_PATH

  last_processed_cl_file = open(LAST_OPT_SIZE_CL_PATH, "r")
  last_optimized_size_cl = int(last_processed_cl_file.read())
  last_processed_cl_file.close()
  return last_optimized_size_cl


def process_cl(cl):
  """Record optimized test sizes at the given cl."""
  global DATA_DIR_PATH
  global LAST_OPT_SIZE_CL_PATH
  global MANAGED_GOOGLE3_PATH

  print "    processing CL " + str(cl)
  get_cmd_output(
      ["git5", "sync", "@" + str(cl), "--rebase"], cwd=MANAGED_GOOGLE3_PATH)

  try:
    print "      blaze building optimized tests"
    build_optimized_tests(MANAGED_GOOGLE3_PATH)

    optimized_size_change_count = 0
    for (test_name, js_file) in get_js_files_by_test_name():
      absolute_js_file_path = MANAGED_GOOGLE3_PATH + "/" + js_file
      if not os.path.isfile(absolute_js_file_path):
        # This test does not exist in the managed repo at the CL at which
        # it is synced. Skip it
        continue

      optimized_size = os.path.getsize(absolute_js_file_path)
      optimized_size_log_path = (
          "%s/%s_optimized_size.dat" % (DATA_DIR_PATH, test_name))

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
  except BashInvocationError:
    print "Failed to blaze build @%s, skipping the unanalyzable CL." % str(cl)

  # Record progress
  last_processed_cl_file = open(LAST_OPT_SIZE_CL_PATH, "w+")
  last_processed_cl_file.write(str(cl))
  last_processed_cl_file.close()


def update_sizes_cache():
  """Caches optimized test sizes for newly seen committed cls."""
  print "  Refreshing the size cache (requires clsearch)."
  last_processed_cl = get_last_processed_cl()
  unprocessed_cls = find_key_cls_since(last_processed_cl)
  for unprocessed_cl in unprocessed_cls:
    process_cl(unprocessed_cl)
  print "  Size cache is fresh."


def validate_environment():
  """Ensure expected directories exist."""
  global MANAGED_REPO_PATH
  global DATA_DIR_PATH
  global LAST_OPT_SIZE_CL_PATH

  if not os.path.isdir(MANAGED_REPO_PATH):
    print("  Creating managed opt size tracking git5 repo at '%s'" %
          MANAGED_REPO_PATH)
    os.mkdir(MANAGED_REPO_PATH)
    get_cmd_output(
        ["git5", "start", "base", "//depot/google3/third_party/java_src/j2cl"],
        cwd=MANAGED_REPO_PATH)

  if not os.path.isdir(DATA_DIR_PATH):
    print("  Creating managed opt size tracking data dir at '%s'" %
          DATA_DIR_PATH)
    os.mkdir(DATA_DIR_PATH)

  if not os.path.isfile(LAST_OPT_SIZE_CL_PATH):
    first_cl = 95664485
    print "  Starting tracking at cl %s" % first_cl
    last_processed_cl_file = open(LAST_OPT_SIZE_CL_PATH, "w+")
    last_processed_cl_file.write(str(first_cl))
    last_processed_cl_file.close()


def make_size_report():
  """Compare current test sizes and generate a report."""
  size_report_file = open(
      os.path.join(
          os.path.dirname(__file__), "size_report.txt"),
      "w+")

  build_optimized_tests()

  synced_to_cl = compute_synced_to_cl()

  size_report_file.write("Report generated by %s at %s.\n" %
                         (getpass.getuser(), time.strftime("%c")))

  size_report_file.write("Synced @%s.\n" % synced_to_cl)

  print "  Comparing sizes."
  last_total_size = 0
  total_size = 0
  all_reports = []
  new_reports = []
  shrinkage_reports = []
  expansion_reports = []
  optimized_size_change_count = 0
  for (test_name, js_file) in get_js_files_by_test_name():
    optimized_size = os.path.getsize(js_file)
    optimized_size_log_path = (
        "%s/%s_optimized_size.dat" % (DATA_DIR_PATH, test_name))

    if os.path.isfile(optimized_size_log_path):
      # compare old and new size
      optimized_size_log_file = open(optimized_size_log_path, "r")

      last_optimized_size = find_last_stats(
          optimized_size_log_file.readlines(), synced_to_cl)

      # This can only happen if you have synced back in time such that the
      # optimized size cache does already contain a file for your new
      # tests but that file does not contain any entries older than the
      # cl at which you are synced.
      if last_optimized_size == 0:
        # record initial size
        optimized_size_change_count += 1
        message = "  '%s' is %s bytes\n" % (test_name, optimized_size)
        all_reports.append(message)
        new_reports.append(message)
        continue

      last_total_size += last_optimized_size
      total_size += optimized_size

      if optimized_size > last_optimized_size:
        optimized_size_change_count += 1
        increased_percent = (
            optimized_size / float(last_optimized_size) - 1) * 100
        message = (
            "  '%s' %s->%s bytes (+%2.2f%%)\n" %
            (test_name, last_optimized_size, optimized_size,
             increased_percent))
        all_reports.append(message)
        expansion_reports.append((increased_percent, message))
      elif optimized_size < last_optimized_size:
        optimized_size_change_count += 1
        decreased_percent = (
            1 - optimized_size / float(last_optimized_size)) * 100
        message = ("  '%s' %s->%s bytes (-%2.2f%%)\n" %
                   (test_name, last_optimized_size, optimized_size,
                    decreased_percent))
        all_reports.append(message)
        shrinkage_reports.append((decreased_percent, message))
    else:
      # record initial size
      optimized_size_change_count += 1
      message = "  '%s' is %s bytes\n" % (test_name, optimized_size)
      all_reports.append(message)
      new_reports.append(message)

  # Keep a maximum of 4 of the largest shrinkages.
  shrinkage_reports = (
      sorted(shrinkage_reports, key=lambda report: report[0], reverse=True)
      [0: 4])
  # Keep a maximum of 4 of the largest expansions.
  expansion_reports = (
      sorted(expansion_reports, key=lambda report: report[0], reverse=False)
      [0: 4])

  size_report_file.write(
      "There are %s size changes.\n" % optimized_size_change_count)

  if total_size != last_total_size:
    total_percent = (
        total_size / float(last_total_size)) * 100
    size_report_file.write(
        "Total size (of already existing tests) "
        "changed from %s to %s bytes (100%%->%2.2f%%).\n" %
        (last_total_size, total_size, total_percent))
  else:
    size_report_file.write(
        "Total size (of already existing tests) did not change.\n")

  size_report_file.write("\n")
  size_report_file.write("\n")
  size_report_file.write("New reports:\n")
  size_report_file.write("**************************************\n")
  if new_reports:
    for new_report in new_reports:
      size_report_file.write(new_report)
  else:
    size_report_file.write("  none\n")

  size_report_file.write("\n")
  size_report_file.write("\n")
  size_report_file.write("Shrinkage report highlights:\n")
  size_report_file.write("**************************************\n")
  if shrinkage_reports:
    for shrinkage_report in shrinkage_reports:
      size_report_file.write(shrinkage_report[1])
  else:
    size_report_file.write("  none\n")

  size_report_file.write("\n")
  size_report_file.write("\n")
  size_report_file.write("Expansion report highlights:\n")
  size_report_file.write("**************************************\n")
  if expansion_reports:
    for expansion_report in expansion_reports:
      size_report_file.write(expansion_report[1])
  else:
    size_report_file.write("  none\n")

  size_report_file.write("\n")
  size_report_file.write("\n")
  size_report_file.write("All reports:\n")
  size_report_file.write("**************************************\n")
  if all_reports:
    for all_report in all_reports:
      size_report_file.write(all_report)
  else:
    size_report_file.write("  none\n")
  size_report_file.close()
  print "  Closing report (%s)" % size_report_file.name


def main():
  print "Generating the size change report:"

  validate_environment()
  update_sizes_cache()
  make_size_report()


main()
