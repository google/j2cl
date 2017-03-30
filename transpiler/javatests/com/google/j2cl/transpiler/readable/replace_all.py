#!/usr/grte/v4/bin/python2.7
#
# Copyright 2015 Google Inc. All Rights Reserved.
"""Regenerates readable JS and build logs."""

import os
import re
from subprocess import PIPE
from subprocess import Popen

from google3.pyglib import app
from google3.pyglib import flags

FLAGS = flags.FLAGS

flags.DEFINE_boolean("nologs", False, "skips jscompiler step.")
flags.DEFINE_string("example_name", "*",
                    "only process examples matched by this regexp")
flags.DEFINE_boolean("skip_integration", False, "only build readables.")

# pylint: disable=global-variable-not-assigned
INTEGRATION_TARGET_PATTERN = ("third_party/java_src/j2cl/transpiler/javatests/"
                              "com/google/j2cl/transpiler/integration/...:all")
JAVA_DIR = "third_party/java_src/j2cl/transpiler/javatests"
EXAMPLES_DIR = JAVA_DIR + "/com/google/j2cl/transpiler/readable/"
READABLE_TARGET_PATTERN = ("third_party/java_src/j2cl/transpiler/javatests/"
                           "com/google/j2cl/transpiler/readable/...")
SUCCESS_CODE = 0


class CmdExecutionError(Exception):
  """Indicates that a cmd execution returned a non-zero exit code."""


def readable_binary_target(readable_target_name):
  return (EXAMPLES_DIR + readable_target_name + ":" +
          readable_target_name + "_binary")


def readable_j2cl_target(readable_target_name):
  return (EXAMPLES_DIR + readable_target_name + ":" +
          readable_target_name + "_j2cl_transpile")


def extract_pattern(pattern_string, from_value):
  """Returns the regex matched value."""
  return re.compile(pattern_string).search(from_value).group(1)


def replace_pattern(pattern_string, replacement, in_value):
  """Returns the regex replaced value."""
  return re.compile(pattern_string).sub(replacement, in_value)


def run_cmd_get_output(cmd_args, include_stderr=False, cwd=None, shell=False):
  """Runs a cmd command and returns output as a string."""
  global SUCCESS_CODE

  process = (Popen(
      cmd_args, shell=shell, stdin=PIPE, stdout=PIPE, stderr=PIPE, cwd=cwd))
  results = process.communicate()
  output = results[0]
  if include_stderr:
    output = (output + "\n" if output else "") + results[1]
  if process.wait() != SUCCESS_CODE:
    raise CmdExecutionError("cmd invocation " + str(cmd_args) +
                            " failed with\n" + results[1])

  return output


def get_readable_target_names():
  """Finds and returns the names of readable targets."""
  global READABLE_TARGET_PATTERN

  example_name_re = re.compile(".*/readable/" +
                               FLAGS.example_name.replace("*", ".*") +
                               ":.*")

  test_targets = (run_cmd_get_output(
      ["blaze", "query", "filter('.*:.*_j2cl_transpile', kind(%s, %s))" %
       ("j2cl_transpile", READABLE_TARGET_PATTERN)]).split("\n"))
  test_targets = [item for item in test_targets
                  if bool(item) and example_name_re.match(item)]

  return [
      extract_pattern(".*readable/(.*?):.*_j2cl_transpile", size_target)
      for size_target in test_targets
  ]


def blaze_build(target_names, build_all):
  """Blaze build everything in 1-go, for speed."""
  global INTEGRATION_TARGET_PATTERN

  args = ["blaze", "build"]
  args += [readable_j2cl_target(target_name) for target_name in target_names]

  if not FLAGS.nologs:
    readable_binary_targets = [readable_binary_target(target_name)
                               for target_name in target_names]

    # Build both all readable targets in one build command so that
    # blaze build parallelizes all the work. Saves a lot of time.
    args += readable_binary_targets

    if build_all and not FLAGS.skip_integration:
      args += [INTEGRATION_TARGET_PATTERN]

  return run_cmd_get_output(args, include_stderr=True)


def replace_transpiled_js(target_names):
  """Copy and reformat and replace with Blaze built JS."""

  # Copy all the zips in one COPY command so that some of the
  # network communication is parallelized.
  if not os.path.isdir("/tmp/js.zip"):
    os.mkdir("/tmp/js.zip")
  run_cmd_get_output(
      ["cp -f blaze-bin/%s**/*.js.zip /tmp/js.zip" % EXAMPLES_DIR], shell=True)

  for target_name in target_names:
    zip_file_path = "/tmp/js.zip/%s_j2cl_transpile.js.zip" % target_name
    extractDir = JAVA_DIR + "/"

    find_command_js_map_sources = ["find", EXAMPLES_DIR + target_name,
                                   "-name", "*.js.map"]

    # Remove old map files before unzipping the new ones
    run_cmd_get_output(find_command_js_map_sources
                       + ["-exec", "rm", "{}", ";"])

    files_to_copy = ["*.js", "*.js.map"]

    run_cmd_get_output(
        ["unzip", "-o", "-d", extractDir, zip_file_path] + files_to_copy)

    find_command_js_sources = ["find", EXAMPLES_DIR + target_name,
                               "-name", "*.java.js"]

    find_command_js_test_sources = ["find", EXAMPLES_DIR + target_name,
                                    "-name", "*.js.txt"]

    # Format .js files
    run_cmd_get_output(find_command_js_sources +
                       ["-exec", "/usr/bin/clang-format", "-i", "{}", "+"])

    # Remove the old .js.txt files (results from the last run)
    run_cmd_get_output(find_command_js_test_sources +
                       ["-exec", "rm", "{}", ";"])

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
    build_log = build_log.replace(
        "blaze-out/gcc-4.X.Y-crosstool-v18-hybrid-grtev4-k8-fastbuild/bin/", "")
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
  build_all = FLAGS.example_name == "*"

  print "Generating readable JS and build logs:"
  readable_target_names = get_readable_target_names()

  if not build_all:
    print "\n    ".join(["  (restricted to):"] + readable_target_names)

  print "  Blaze building everything"
  if FLAGS.nologs:
    print "     (skipping logs)"
  build_log = blaze_build(readable_target_names, build_all)

  print "  Copying and reformatting transpiled JS"
  replace_transpiled_js(readable_target_names)

  if FLAGS.nologs:
    print "  Skipping logs!!!"
  else:
    print "  Processing build logs"
    gather_closure_warnings(build_log)

  print "run 'git gui' to see changes"


if __name__ == "__main__":
  app.run()
