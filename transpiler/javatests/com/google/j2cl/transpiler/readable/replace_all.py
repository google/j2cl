#!/usr/bin/python2.7
#
# Copyright 2015 Google Inc. All Rights Reserved.

"""Regenerates readable JS and build logs."""


from os import listdir
from os import walk
from os.path import isfile
from os.path import join
import re
import shutil
from subprocess import PIPE
from subprocess import Popen


# pylint: disable=global-variable-not-assigned
JAVA_DIR = "third_party/java_src/j2cl/transpiler/javatests"
EXAMPLES_DIR = JAVA_DIR + "/com/google/j2cl/transpiler/readable/"
READABLE_TARGET_PATTERN = ("third_party/java_src/j2cl/transpiler/javatests/"
                           "com/google/j2cl/transpiler/readable/...")
SUCCESS_CODE = 0


class CmdExecutionError(Exception):
  """Indicates that a cmd execution returned a non-zero exit code."""


def extract_pattern(pattern_string, from_value):
  """Returns the regex matched value."""
  return re.compile(pattern_string).search(from_value).group(1)


def run_cmd_get_output(cmd_args, include_stderr=False, cwd=None):
  """Runs a cmd command and returns output as a string."""
  global SUCCESS_CODE

  process = Popen(cmd_args, stdin=PIPE, stdout=PIPE, stderr=PIPE, cwd=cwd)
  results = process.communicate()
  output = results[0]
  if include_stderr:
    output = (output + "\n" if output else "") + results[1]
  if process.wait() != SUCCESS_CODE:
    raise CmdExecutionError(
        "cmd invocation " + str(cmd_args) + " failed with\n" + results[1])

  return output


def get_js_binary_file_paths():
  """Finds and returns a list of js_binary bundle js file paths."""
  # Gather a list of the names of the test targets we care about
  test_targets = (
      run_cmd_get_output(
          ["blaze", "query",
           "filter('.*_binary', kind(%s, %s))" %
           ("js_binary", READABLE_TARGET_PATTERN)]).splitlines())
  test_targets = filter(bool, test_targets)

  return [
      size_target.replace("//", "blaze-bin/").replace(":", "/") + "-bundle.js"
      for size_target in test_targets]


def get_transpiled_js_file_paths():
  """Finds and returns transpiled js file paths."""
  global EXAMPLES_DIR

  example_names = [
      example_name for example_name in listdir(EXAMPLES_DIR)
      if not isfile(EXAMPLES_DIR + example_name)]
  example_js_file_paths = []

  for example_name in example_names:
    for root, _, files in walk(EXAMPLES_DIR + example_name):
      example_js_file_paths += [
          join(root, f)[: -5] + ".js"
          for f in files if f.endswith(".java")]

  return example_js_file_paths


def blaze_build_all():
  """Blaze build everything in 1-go, for speed."""
  return run_cmd_get_output(["blaze", "build", EXAMPLES_DIR + "..."])


def replace_transpiled_js():
  """Copy and reformat and replace with Blaze built JS."""
  for js_file_path in get_transpiled_js_file_paths():
    shutil.move("blaze-bin/" + js_file_path, js_file_path)
    run_cmd_get_output(["clang-format", "-style=Google", "-i", js_file_path])
    shutil.move(js_file_path, js_file_path + ".txt")


def gather_closure_warnings():
  """Gather Closure compiler warnings.

  Deletes just part of Blaze's cache so that it is forced to rebuild just the
  js_binary targets. The resulting build logs are split and saved.
  """
  run_cmd_get_output(["rm", "-fr"] + get_js_binary_file_paths())

  build_logs = run_cmd_get_output(
      ["blaze", "build", EXAMPLES_DIR + "..."],
      include_stderr=True)
  build_logs = build_logs.split("____From Compiling JavaScript ")[1:]
  build_logs = filter(None, build_logs)
  for build_log in build_logs:
    # Remove unstable build timing lines.
    build_log = "\n".join([
        line for line in build_log.splitlines()
        if not line.startswith("_") and
        "  Compiling" not in line and
        "Running" not in line])

    # Filter out the unstable ", ##% typed" message
    percent_typed_msg = (
        extract_pattern(r"g\(s\)(, .*? typed)", build_log))
    build_log = build_log.replace(percent_typed_msg, "")

    build_log_path = extract_pattern("//(.*?):", build_log) + "/build.log"
    with open(build_log_path, "w") as build_log_file:
      build_log_file.write(build_log)


def main():
  print "Generating readable JS and build logs:"

  print "  Blaze building everything"
  blaze_build_all()

  print "  Copying and reformatting transpiled JS"
  replace_transpiled_js()

  print "  Re-Closure compiling examples to gather logs"
  gather_closure_warnings()

  print "run 'git gui' to see changes"

main()
