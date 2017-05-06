#!/usr/bin/python2.7
#
# Copyright 2015 Google Inc. All Rights Reserved.
"""Reports optimized size changes caused by the current CL."""

from multiprocessing import Pool
import os
import signal
import zlib

import repo_util


def get_gzip_size(content):
  gzip_level = 6  # matches command line gzip
  compressed_content = zlib.compress(content, gzip_level)
  return len(compressed_content)


def print_table_header(size_report_file):
  header_format = "  %7s%7s\n"
  size_report_file.write(header_format % ("old", "new"))


def console_log(message):
  """Prints a string to the console."""
  # Necessary because lambda methods can not invoke print statements.
  print message


def create_pool():
  """Create a pool that does not capture ctrl-c."""
  original_sigint_handler = signal.signal(signal.SIGINT, signal.SIG_IGN)
  pool = Pool(processes=2)
  signal.signal(signal.SIGINT, original_sigint_handler)
  return pool


def make_size_report():
  """Compare current test sizes and generate a report."""
  row_format = "  %7s%7s %s (%s)\n"

  file_name = os.path.join(os.path.dirname(__file__), "size_report.txt")
  repo_util.check_out_file(file_name)
  size_report_file = open(file_name, "w+")

  synced_to_cl = repo_util.compute_synced_to_cl()
  repo_util.managed_repo_sync_to(synced_to_cl)

  size_report_file.write("Integration tests optimized size report:\n")
  size_report_file.write("**************************************\n")

  print "  Building original and modified targets."

  pool = create_pool()

  original_result = pool.apply_async(
      repo_util.build_optimized_tests, [repo_util.get_managed_path()],
      callback=lambda x: console_log("    Original done building."))
  modified_result = pool.apply_async(
      repo_util.build_optimized_tests,
      callback=lambda x: console_log("    Modified done building."))
  pool.close()
  pool.join()

  # Invoke get() on async results to "propagate" the exceptions that
  # were raised if any.
  original_result.get()
  modified_result.get()

  print "  Collecting original and modified optimized sizes."
  original_js_files_by_test_name = (
      repo_util.get_js_files_by_test_name(repo_util.get_managed_path()))
  modified_js_files_by_test_name = repo_util.get_js_files_by_test_name()

  print "  Comparing results."
  # Collect all test names
  all_test_names = set()
  all_test_names.update(original_js_files_by_test_name.keys())
  all_test_names.update(modified_js_files_by_test_name.keys())
  all_test_names = sorted(list(all_test_names))

  original_total_size = 0
  modified_total_size = 0
  original_total_size_gzip = 0
  modified_total_size_gzip = 0
  size_change_count = 0
  all_reports = []
  new_reports = []

  for test_name in all_test_names:
    original_js_file = original_js_files_by_test_name.get(test_name)
    modified_js_file = modified_js_files_by_test_name.get(test_name)

    if original_js_file:
      original_js_file = repo_util.get_managed_path() + "/" + original_js_file

    original_size = os.path.getsize(
        original_js_file) if original_js_file else -1
    original_size_gzip = get_gzip_size(
        open(original_js_file).read()) if original_js_file else -1
    modified_size = os.path.getsize(
        modified_js_file) if modified_js_file else -1
    modified_size_gzip = get_gzip_size(
        open(modified_js_file).read()) if modified_js_file else -1

    # If this is a pre-existing test
    if original_size >= 0:
      # Log it's prexisting size
      original_total_size += original_size
      original_total_size_gzip += original_size_gzip
      # And if an updated size exists then
      if modified_size >= 0:
        # Log that too
        modified_total_size += modified_size
        modified_total_size_gzip += modified_size_gzip

    # If the original optimized JS file exists
    if original_size >= 0:
      # If the modified optimized JS file exists
      if modified_size >= 0:
        # Both files exist, so compare their sizes.
        size_percent = (modified_size / float(original_size)) * 100
        size_percent_gzip = (modified_size_gzip /
                             float(original_size_gzip)) * 100
        note = ("unchanged" if original_size_gzip == modified_size_gzip else
                "%.2f%%" % (size_percent_gzip - 100))
        message = (row_format %
                   (original_size_gzip, modified_size_gzip, test_name, note))
        all_reports.append((size_percent_gzip, message))

        if (modified_size != original_size or
            modified_size_gzip != original_size_gzip):
          size_change_count += 1
    else:
      if modified_size >= 0:
        # The original optimized JS file doesn't exist and the
        # modified one does, this is a new result.
        size_change_count += 1
        size_percent = 100
        size_percent_gzip = 100
        note = "new"
        message = (row_format %
                   (original_size_gzip, modified_size_gzip, test_name, note))
        all_reports.append((size_percent, message))
        new_reports.append(message)

  # Keep a maximum of 4 of the largest shrinkages.
  shrinkage_reports = (
      sorted(all_reports, key=lambda report: report[0], reverse=False)[0:4])
  shrinkage_reports = [
      report for report in shrinkage_reports if report[0] < 100
  ]
  # Keep a maximum of 4 of the largest expansions.
  expansion_reports = (
      sorted(all_reports, key=lambda report: report[0], reverse=True)[0:4])
  expansion_reports = [
      report for report in expansion_reports if report[0] > 100
  ]

  size_report_file.write("There are %s size changes.\n" % size_change_count)

  if modified_total_size != original_total_size:
    total_percent = (modified_total_size / float(original_total_size)) * 100
    total_percent_gzip = (modified_total_size_gzip /
                          float(original_total_size_gzip)) * 100
    size_report_file.write(
        "Total size (of already existing tests) "
        "changed from\n  %s to %s bytes (100%%->%2.2f%%) and from\n  "
        "%s to %s bytes (100%%->%2.2f%%) gzipped.\n" %
        (original_total_size, modified_total_size, total_percent,
         original_total_size_gzip, modified_total_size_gzip,
         total_percent_gzip))
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
    print_table_header(size_report_file)
    for shrinkage_report in shrinkage_reports:
      size_report_file.write(shrinkage_report[1])
  else:
    size_report_file.write("  none\n")

  size_report_file.write("\n")
  size_report_file.write("\n")
  size_report_file.write("Expansion report highlights:\n")
  size_report_file.write("**************************************\n")
  if expansion_reports:
    print_table_header(size_report_file)
    for expansion_report in expansion_reports:
      size_report_file.write(expansion_report[1])
  else:
    size_report_file.write("  none\n")

  size_report_file.write("\n")
  size_report_file.write("\n")
  size_report_file.write("All reports:\n")
  size_report_file.write("**************************************\n")
  if all_reports:
    print_table_header(size_report_file)
    for size_percent, report in all_reports:
      size_report_file.write(report)
  else:
    size_report_file.write("  none\n")
  size_report_file.close()
  print "  Closing report (%s)" % size_report_file.name


def main():
  print "Generating the size change report:"

  repo_util.managed_repo_validate_environment()
  make_size_report()


main()
