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

from __future__ import print_function
import os
import repo_util


size_format = "  %9s%9s"


def print_table_header(size_report_file):
  size_report_file.write((size_format + "\n") % ("old", "new"))


def make_size_report(path_name, original_bundled_targets, original_opt_targets,
                     modified_bundled_targets, modified_opt_targets):
  """Compare current test sizes and generate a report."""

  size_report_file = open(path_name, "w+")

  size_report_file.write("Integration tests size report:\n")
  size_report_file.write("**************************************\n")

  print("  Building original and modified targets.")

  original_targets = original_bundled_targets + original_opt_targets
  modified_targets = modified_bundled_targets + modified_opt_targets
  repo_util.build_original_and_modified(original_targets, modified_targets)

  print("  Collecting original and modified sizes.")

  bundled_by_test_name = repo_util.get_files_by_test_name(
      modified_bundled_targets
  )
  optimized_by_test_name = repo_util.get_files_by_test_name(
      modified_opt_targets
  )

  print("  Comparing results.")

  uncompiled_reports = []

  for test_name in sorted(bundled_by_test_name.keys()):
    test = bundled_by_test_name.get(test_name)
    original, modified = get_files(test)
    uncompiled_reports.append(
        create_report(test_name, get_size(original), get_size(modified))
    )

  original_total_size = 0
  modified_total_size = 0
  all_reports = []

  incremental_reports = []

  for test_name in sorted(optimized_by_test_name.keys()):
    test = optimized_by_test_name.get(test_name)
    original, modified = get_files(test)
    original_size = repo_util.get_compressed_size(original)
    modified_size = repo_util.get_compressed_size(modified)

    existing_target = os.path.exists(original)
    if existing_target:
      modified_total_size += modified_size
      original_total_size += original_size

    all_reports.append(create_report(test_name, original_size, modified_size))

    incremental = optimized_by_test_name.get("%s_inc%s%s" %
                                             test_name.partition("/"))
    if incremental:
      inc_original, inc_modified = get_files(incremental)
      original_increment = (
          get_size(inc_original) - get_size(original) if existing_target else -1
      )
      modified_increment = get_size(inc_modified) - get_size(modified)
      incremental_reports.append(
          create_report(test_name, original_increment, modified_increment)
      )

  changed_reports = [report for report in all_reports if report[0] != 100]
  shrinkage_reports = [report for report in changed_reports if report[0] < 100]
  expansion_reports = [report for report in changed_reports if report[0] > 100]

  size_report_file.write(
      "There are %s optimized size changes.\n" % len(changed_reports))

  if modified_total_size != original_total_size:
    total_percent = (modified_total_size / float(original_total_size)) * 100
    size_report_file.write(
        "Total optimized size (of already existing tests) "
        "changed from\n  %s to %s bytes (100%%->%2.1f%%).\n" %
        (original_total_size, modified_total_size, total_percent))
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
  size_report_file.write("Incremental size reports (non-compressed):\n")
  size_report_file.write("**************************************\n")
  print_table(size_report_file, incremental_reports)

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

  print("  Closing report (%s)" % size_report_file.name)
  size_report_file.close()


def get_files(test):
  modified = "blaze-bin/" + test
  original = repo_util.get_j2size_repo_path() + "/" + modified
  return (original, modified)


def get_size(filename):
  return os.path.getsize(filename) if os.path.exists(filename) else -1


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
    # The original file doesn't exist, this is a new result.
    size_percent = 100
    note = "new"

  row_format = size_format + " %s (%s)\n"
  message = row_format % (original_size, modified_size, test_name, note)

  return (size_percent, message)


optimized_tests_file_header = """\"\"\"DO NOT EDIT - generated by make_size_report.py
\"\"\"

OPTIMIZED_JS_LIST = [
"""


def make_optimized_test_list(optimized_tests):
  """Creates a .bzl file that has all the test targets."""
  optimized_tests_file = open(repo_util.TEST_LIST, "w+")

  optimized_tests_file.write(optimized_tests_file_header)
  for test in sorted(optimized_tests):
    optimized_tests_file.write("    \"")
    optimized_tests_file.write(test)
    optimized_tests_file.write("\",\n")
  optimized_tests_file.write("]\n")

  optimized_tests_file.close()
  print("  Generated test file list (%s)" % optimized_tests_file.name)


def main(unused_argv):
  print("Generating the size change report:")

  repo_util.sync_j2size_repo()

  (original_bundled, original_opt) = repo_util.get_all_size_tests(
      repo_util.get_j2size_repo_path())
  (modified_bundled, modified_opt) = repo_util.get_all_size_tests()

  make_size_report(repo_util.SIZE_REPORT, original_bundled, original_opt,
                   modified_bundled, modified_opt)

  make_optimized_test_list(modified_opt)
