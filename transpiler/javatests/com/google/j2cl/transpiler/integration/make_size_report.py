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

"""Reports size changes caused by the current CL."""

from multiprocessing import Pool
import os
import signal
import zlib

import repo_util


size_format = "  %9s%9s"


def get_gzip_size(file_name):
  gzip_level = 6  # matches command line gzip
  compressed_content = zlib.compress(open(file_name).read(), gzip_level)
  return len(compressed_content)


def print_table_header(size_report_file):
  size_report_file.write((size_format + "\n") % ("old", "new"))


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


def make_size_report(file_name, original_targets, modified_targets,
                     uncompiled_targets):
  """Compare current test sizes and generate a report."""

  path_name = os.path.join(os.path.dirname(__file__), file_name)
  repo_util.check_out_file(path_name)
  size_report_file = open(path_name, "w+")

  synced_to_cl = repo_util.compute_synced_to_cl()
  repo_util.managed_repo_sync_to(synced_to_cl)

  size_report_file.write("Integration tests size report:\n")
  size_report_file.write("**************************************\n")

  print "  Building original and modified targets."

  pool = create_pool()

  original_result = pool.apply_async(
      repo_util.build_tests,
      [original_targets, repo_util.get_managed_path()],
      callback=lambda x: console_log("    Original done building."))
  modified_result = pool.apply_async(
      repo_util.build_tests, [modified_targets],
      callback=lambda x: console_log("    Modified done building."))
  pool.close()
  pool.join()

  # Invoke get() on async results to "propagate" the exceptions that
  # were raised if any.
  original_result.get()
  modified_result.get()

  print "  Collecting original and modified sizes."
  uncompiled_js_files_by_test_name = repo_util.get_js_files_by_test_name(
      uncompiled_targets, True)
  optimized_js_files_by_test_name = repo_util.get_js_files_by_test_name(
      modified_targets, False)

  print "  Comparing results."

  uncompiled_reports = []

  for test_name in sorted(uncompiled_js_files_by_test_name.keys()):
    modified_js_file = uncompiled_js_files_by_test_name.get(test_name)
    original_js_file = repo_util.get_managed_path() + "/" + modified_js_file
    uncompiled_reports.append(
        create_report(test_name,
                      os.path.getsize(original_js_file),
                      os.path.getsize(modified_js_file)))
    uncompiled_reports.append(
        create_report(test_name + ".minified",
                      repo_util.get_minimized_size(original_js_file),
                      repo_util.get_minimized_size(modified_js_file)))

  original_total_size = 0
  modified_total_size = 0
  original_total_size_gzip = 0
  modified_total_size_gzip = 0
  all_reports = []

  for test_name in sorted(optimized_js_files_by_test_name.keys()):
    modified_js_file = optimized_js_files_by_test_name.get(test_name)
    original_js_file = repo_util.get_managed_path() + "/" + modified_js_file

    existing_target = os.path.exists(original_js_file)

    modified_size_gzip = get_gzip_size(modified_js_file)
    original_size_gzip = get_gzip_size(
        original_js_file) if existing_target else -1

    if existing_target:
      modified_total_size += os.path.getsize(modified_js_file)
      original_total_size += os.path.getsize(original_js_file)
      modified_total_size_gzip += modified_size_gzip
      original_total_size_gzip += original_size_gzip

    report = create_report(test_name, original_size_gzip, modified_size_gzip)

    if report:
      all_reports.append(report)

  changed_reports = [report for report in all_reports if report[0] != 100]
  shrinkage_reports = [report for report in changed_reports if report[0] < 100]
  expansion_reports = [report for report in changed_reports if report[0] > 100]

  size_report_file.write(
      "There are %s optimized size changes.\n" % len(changed_reports))

  if modified_total_size != original_total_size:
    total_percent = (modified_total_size / float(original_total_size)) * 100
    total_percent_gzip = (modified_total_size_gzip /
                          float(original_total_size_gzip)) * 100
    size_report_file.write(
        "Total optimized size (of already existing tests) "
        "changed from\n  %s to %s bytes (100%%->%2.1f%%) and from\n  "
        "%s to %s bytes (100%%->%2.1f%%) gzipped.\n" %
        (original_total_size, modified_total_size, total_percent,
         original_total_size_gzip, modified_total_size_gzip,
         total_percent_gzip))
  else:
    size_report_file.write(
        "Total size (of already existing tests) did not change.\n")

  size_report_file.write("\n")
  size_report_file.write("\n")
  size_report_file.write("Shrinkage report highlights:\n")
  size_report_file.write("**************************************\n")
  print_table(size_report_file,
              sorted(shrinkage_reports, key=lambda r: r[0], reverse=False)[0:4])

  size_report_file.write("\n")
  size_report_file.write("\n")
  size_report_file.write("Expansion report highlights:\n")
  size_report_file.write("**************************************\n")
  print_table(size_report_file,
              sorted(expansion_reports, key=lambda r: r[0], reverse=True)[0:4])

  size_report_file.write("\n")
  size_report_file.write("\n")
  size_report_file.write("Uncompiled bundle reports:\n")
  size_report_file.write("**************************************\n")
  print_table(size_report_file, uncompiled_reports)

  size_report_file.write("\n")
  size_report_file.write("\n")
  size_report_file.write("All reports:\n")
  size_report_file.write("**************************************\n")
  print_table(size_report_file, all_reports)

  size_report_file.close()
  print "  Closing report (%s)" % size_report_file.name


def print_table(size_report_file, reports):
  if reports:
    print_table_header(size_report_file)
    for report in reports:
      size_report_file.write(report[1])
  else:
    size_report_file.write("  none\n")


def create_report(test_name, original_size, modified_size):
  """Generate a report for a single test."""

  if original_size >= 0:
    # Both files exist, so compare their sizes.
    size_percent = (modified_size / float(original_size)) * 100
    note = "unchanged" if size_percent == 100 else "%.1f%%" % (
        size_percent - 100)
  else:
    # The original JS file doesn't exist, this is a new result.
    size_percent = 100
    note = "new"

  row_format = size_format + " %s (%s)\n"
  message = row_format % (original_size, modified_size, test_name, note)

  return (size_percent, message)


def main():
  repo_util.managed_repo_validate_environment()

  print "Generating the size change report:"
  original_targets = repo_util.get_all_optimized_tests(
      repo_util.get_managed_path())
  modified_targets = repo_util.get_all_optimized_tests()
  uncompiled_targets = [
      repo_util.get_optimized_test("box2d_default"),
      repo_util.get_optimized_test("emptyclass"),
  ]
  make_size_report(
      "size_report.txt", original_targets, modified_targets, uncompiled_targets)

main()
