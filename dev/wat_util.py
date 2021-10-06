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
"""Util funcs for manimpulating wasm files."""


def filter_wat_file(input_path, output_path, java_package):
  """Keep lines of code related to the readable examples.

  Args:
    input_path: Location of the wat file to filter.
    output_path: Write location may be the same as input_path.
    java_package: The target java package to filter too.
  """

  filtered_lines = []
  # skip lines until we find a compilation unit from the example
  skip_line = True

  with open(input_path, "r") as wat_file:
    for line in wat_file:
      trimmed_line = line.lstrip()
      if trimmed_line.startswith(";;; Code for %s" % java_package):
        # Add a new line before the comment to clearly see sections in the
        # readable
        if filtered_lines:
          filtered_lines.append("\n")
        skip_line = False
      elif trimmed_line.startswith(";;; Code for ") or trimmed_line.startswith(
          ";;; End of "):
        skip_line = True

      if not skip_line:
        filtered_lines.append(line)

  with open(output_path, "w") as wat_file:
    wat_file.writelines(filtered_lines)
