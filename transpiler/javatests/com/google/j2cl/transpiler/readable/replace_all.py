#!/usr/bin/python2.7
#
# Copyright 2015 Google Inc. All Rights Reserved.

"""Regenerates readable JS and build logs."""


import re
from subprocess import PIPE
from subprocess import Popen


# pylint: disable=global-variable-not-assigned
TEST_TARGET_PATTERN = ("third_party/java_src/j2cl/transpiler/javatests/"
                       "com/google/j2cl/transpiler/readable/...:all")
JAVA_DIR = "third_party/java_src/j2cl/transpiler/javatests"
EXAMPLES_DIR = JAVA_DIR + "/com/google/j2cl/transpiler/readable/"
READABLE_TARGET_PATTERN = ("third_party/java_src/j2cl/transpiler/javatests/"
                           "com/google/j2cl/transpiler/readable/...")
JAVA8_BOOT_CLASS_PATH = ("--javac_bootclasspath="
                         "//third_party/java/jdk:langtools8-bootclasspath")
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


def get_readable_target_names():
  """Finds and returns the names of readable targets."""
  global TEST_TARGET_PATTERN

  test_targets = (
      run_cmd_get_output(
          ["blaze", "query",
           "filter('.*:.*_j2cl_transpile', kind(%s, %s))" %
           ("j2cl_transpile", TEST_TARGET_PATTERN)]).split("\n"))
  test_targets = filter(bool, test_targets)

  return [
      extract_pattern(
          ".*readable/(.*?):.*_j2cl_transpile", size_target)
      for size_target in test_targets]


def blaze_build(target_names):
  """Blaze build everything in 1-go, for speed."""
  args = ["blaze", "build"]
  args += [EXAMPLES_DIR + target_name + ":" + target_name + "_j2cl_transpile"
           for target_name in target_names]
  args += [JAVA8_BOOT_CLASS_PATH]
  return run_cmd_get_output(args)


def replace_transpiled_js(target_names):
  """Copy and reformat and replace with Blaze built JS."""
  pairs = zip(target_names, ["blaze-bin/%s/%s/%s_j2cl_transpile.pintozip" %
                             (EXAMPLES_DIR, target_name, target_name)
                             for target_name in target_names])

  for (target_name, zip_file_path) in pairs:
    run_cmd_get_output(
        ["unzip", "-o", "-d", JAVA_DIR + "/", zip_file_path])

  # Ignore files under natives_sources/ since these are not generated.
  find_command_js_sources = ["find", EXAMPLES_DIR, "-name", "*.js",
                             "-not", "-path", "**/native_sources/*"]

  find_command_js_test_sources = ["find", EXAMPLES_DIR, "-name", "*.js.txt",
                                  "-not", "-path", "**/native_sources/*"]

  # Format .js files
  run_cmd_get_output(
      find_command_js_sources +
      ["-exec", "clang-format", "-i", "{}", "+"])

  # Remove the old .js.txt files (results from the last run)
  run_cmd_get_output(
      find_command_js_test_sources + ["-exec", "rm", "{}", ";"])

  # Move the newly unzipped .js => .js.txt
  run_cmd_get_output(
      find_command_js_sources + ["-exec", "mv", "{}", "{}.txt", ";"])


def gather_closure_warnings():
  """Gather Closure compiler warnings.

  Deletes just part of Blaze's cache so that it is forced to rebuild just the
  js_binary targets. The resulting build logs are split and saved.
  """

  # Delete the old build.log files before we regenerate them.
  find_command_build_logs = ["find", EXAMPLES_DIR, "-name", "build.log"]
  run_cmd_get_output(
      find_command_build_logs + ["-exec", "rm", "{}", ";"])

  run_cmd_get_output(["rm", "-fr"] + get_js_binary_file_paths())

  build_logs = run_cmd_get_output(
      ["blaze", "build", EXAMPLES_DIR + "...", JAVA8_BOOT_CLASS_PATH],
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

    # Remove folder path spam.
    build_log = build_log.replace(
        "blaze-out/gcc-4.X.Y-crosstool-v18-hybrid-grtev4-k8-fastbuild/bin/",
        "")
    # Filter out the unstable ", ##% typed" message
    percent_typed_msg = (
        extract_pattern(r"g\(s\)(, .*? typed)", build_log))
    build_log = build_log.replace(percent_typed_msg, "")

    build_log_path = extract_pattern("//(.*?):", build_log) + "/build.log"
    with open(build_log_path, "w") as build_log_file:
      build_log_file.write(build_log)


def main():
  print "Generating readable JS and build logs:"
  readable_target_names = get_readable_target_names()

  print "  Blaze building everything"
  blaze_build(readable_target_names)

  print "  Copying and reformatting transpiled JS"
  replace_transpiled_js(readable_target_names)

  print "  Re-Closure compiling examples to gather logs"
  gather_closure_warnings()

  print "run 'git gui' to see changes"

main()
