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
"""Run the tasks needed before code submission for review."""

import make_size_report
import replace_all
import test_all


def main(argv):
  # TODO(goktug): Run all the build together to speed up the flow.
  print_header("Regenerating all readable Examples")

  replace_all.run_for_presubmit(argv)

  print_header("Making size report")
  make_size_report.main(argv)

  print_header("Running all integration tests")
  test_all.run_for_presubmit(argv)

  print(
      "\n\nRemember to check for changes in the readable examples and size_report."
  )


def print_header(header_string):
  print("\n#### " + header_string + "  ####\n")
