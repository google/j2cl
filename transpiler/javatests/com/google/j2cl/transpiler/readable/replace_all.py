#!/usr/grte/v4/bin/python2.7

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

"""Regenerates readable JS and build logs."""

import os
import re
from subprocess import PIPE
from subprocess import Popen

from google3.pyglib import app
from google3.pyglib import flags

FLAGS = flags.FLAGS

flags.DEFINE_boolean("logs", True, "skips jscompiler step if false.")
flags.DEFINE_string("name_filter", ".*",
                    "only process readables matched by this regexp")
flags.DEFINE_boolean("skip_integration", False, "only build readables.")

JAVA_DIR = "third_party/java_src/j2cl/transpiler/javatests/"
READABLE_TARGET_PATTERN = JAVA_DIR + "com/google/j2cl/transpiler/readable/..."
INTEGRATION_TARGET_PATTERN = JAVA_DIR + "com/google/j2cl/transpiler/integration/..."


class CmdExecutionError(Exception):
  """Indicates that a cmd execution returned a non-zero exit code."""


def extract_pattern(pattern_string, from_value):
  """Returns the regex matched value."""
  return re.compile(pattern_string).search(from_value).group(1)


def replace_pattern(pattern_string, replacement, in_value):
  """Returns the regex replaced value."""
  return re.compile(pattern_string).sub(replacement, in_value)


def run_cmd_get_output(cmd_args, include_stderr=False, cwd=None, shell=False):
  """Runs a cmd command and returns output as a string."""

  process = (Popen(
      cmd_args, shell=shell, stdin=PIPE, stdout=PIPE, stderr=PIPE, cwd=cwd))
  results = process.communicate()
  output = results[0]
  if include_stderr:
    output = (output + "\n" if output else "") + results[1]
  if process.wait() != 0:
    raise CmdExecutionError("cmd invocation " + str(cmd_args) +
                            " failed with\n" + results[1])

  return output


def get_readable_dirs(name_filter):
  """Finds and returns the dirs of readable examples."""

  readable_dirs = run_cmd_get_output(
      [
          "blaze", "query",
          "filter('%s:readable$', %s)" % (name_filter, READABLE_TARGET_PATTERN),
          "--output=package"
      ]
  ).split("\n")
  return filter(bool, readable_dirs)


def blaze_build(target_dirs, build_integration_tests):
  """Blaze build everything in 1-go, for speed."""

  build_targets = [d + ":readable_j2cl_transpile" for d in target_dirs]
  if FLAGS.logs:
    build_targets += [d + ":readable_binary" for d in target_dirs]

  if build_integration_tests:
    build_targets += [INTEGRATION_TARGET_PATTERN]

  cmd = ["blaze", "build", "-c", "fastbuild"] + build_targets
  return run_cmd_get_output(cmd, include_stderr=True)


def replace_transpiled_js(readable_dirs):
  """Copy and reformat and replace with Blaze built JS."""

  for readable_dir in readable_dirs:
    zip_file_path = "blaze-bin/%s/readable_j2cl_transpile.js.zip" % readable_dir
    output = readable_dir + "/output"

    # Clean the output directory from the result of last run.
    run_cmd_get_output(["rm", "-Rf", output])
    run_cmd_get_output(["mkdir", output])

    # Update the output directory with result of the new run.
    run_cmd_get_output([
        "unzip", "-o", "-d", output, zip_file_path, "-x", "*.java", "-x", "*.map"
    ])
    # Normalize the path relative to output directory.
    run_cmd_get_output([
        "mv " + output + "/" + os.path.relpath(readable_dir, JAVA_DIR) + "/* " + output
    ], shell=True)

    find_command_js_sources = ["find", output, "-name", "*.js"]

    # Format .js files
    run_cmd_get_output(find_command_js_sources +
                       ["-exec", "/usr/bin/clang-format", "-i", "{}", "+"])

    # Move the newly unzipped .js => .js.txt
    run_cmd_get_output(find_command_js_sources +
                       ["-exec", "mv", "{}", "{}.txt", ";"])


def gather_closure_warnings(build_log):
  """Gather Closure compiler warnings."""

  build_logs = build_log.split("____From Compiling JavaScript ")[1:]
  build_logs = filter(None, build_logs)
  for build_log in build_logs:
    # Remove unstable build timing lines.
    build_log = "\n".join([
        line for line in build_log.splitlines()
        if not line.startswith("_") and "  Compiling" not in line and "Running"
        not in line and "Building" not in line
    ])

    # Remove folder path spam.
    build_log = build_log.replace("blaze-out/k8-fastbuild/genfiles/", "")
    # Remove stable (but occasionally changing) line number details.
    build_log = replace_pattern(r"\:([0-9]*)\:", "", build_log)
    # Filter out the unstable ", ##% typed" message
    percent_typed_msg = (extract_pattern(r"g\(s\)(, .*? typed)", build_log))
    build_log = build_log.replace(percent_typed_msg, "")

    build_log_path = extract_pattern("//(.*?):", build_log) + "/build.log"
    # Don't write build.log files for integration tests.
    if "readable/" not in build_log_path:
      continue
    if "\n0 error(s), 0 warning(s)" in build_log:
      # No errors, no warnings, delete the build.log file if it exists.
      if os.path.isfile(build_log_path):
        run_cmd_get_output(["rm", build_log_path])

      continue
    with open(build_log_path, "w") as build_log_file:
      build_log_file.write(build_log)


def main(unused_argv):
  build_all = FLAGS.name_filter == ".*"

  print "Generating readable JS and build logs:"
  readable_dirs = get_readable_dirs(FLAGS.name_filter)
  if build_all:
    print "  Blaze building everything"
  else:
    print "  Blaze building:"
    print "\n".join(["    " + d for d in readable_dirs])

  build_integration_tests = (build_all
                             and FLAGS.logs and not FLAGS.skip_integration)
  build_log = blaze_build(readable_dirs, build_integration_tests)

  print "  Copying and reformatting transpiled JS"
  replace_transpiled_js(readable_dirs)

  if not FLAGS.logs:
    print "  Skipping logs!!!"
  else:
    print "  Processing build logs"
    gather_closure_warnings(build_log)

  print "run diff on repo to see changes"


if __name__ == "__main__":
  app.run()
