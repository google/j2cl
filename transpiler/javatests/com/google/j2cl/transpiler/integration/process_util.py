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

"""Util funcs for running commands and parsing results."""

import re
from subprocess import PIPE
from subprocess import Popen

# pylint: disable=global-variable-not-assigned
SUCCESS_CODE = 0


class CmdExecutionError(Exception):
  """Indicates that a cmd execution returned a non-zero exit code."""


def extract_pattern(pattern_string, from_value):
  """Returns the regex matched value."""
  return re.compile(pattern_string).search(from_value).group(1)


def run_cmd_get_output(cmd_args, cwd=None):
  """Runs a command and returns output as a string."""
  global SUCCESS_CODE

  process = Popen(cmd_args, stdin=PIPE, stdout=PIPE, stderr=PIPE, cwd=cwd)
  output = process.communicate()[0]
  if process.wait() != SUCCESS_CODE:
    raise CmdExecutionError("cmd invocation FAILED: " + " ".join(cmd_args))

  return output
