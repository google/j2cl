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

import argparse
import os
import re
import tempfile
import repo_util

JAVA_DIR = "third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/readable/java/"
READABLE_TARGET_PATTERN = JAVA_DIR + "..."


class CmdExecutionError(Exception):
  """Indicates that a cmd execution returned a non-zero exit code."""


def extract_pattern(pattern_string, from_value):
  """Returns the regex matched value."""
  return re.compile(pattern_string).search(from_value).group(1)


def replace_pattern(pattern_string, replacement, in_value):
  """Returns the regex replaced value."""
  return re.compile(pattern_string).sub(replacement, in_value)


def get_readable_dirs(name_filter, rule_suffix=""):
  """Finds and returns the dirs of readable examples."""
  return _get_dirs_from_blaze_query("%s:readable%s$" %
                                    (name_filter, rule_suffix))


def _get_dirs_from_blaze_query(rules_filter):
  dirs = repo_util.run_cmd([
      "blaze", "query",
      "filter('%s', %s)" % (rules_filter, READABLE_TARGET_PATTERN),
      "--output=package"
  ]).splitlines()
  return list(filter(bool, dirs))


def blaze_clean():
  """Clean output to force full compilation of all targets."""
  repo_util.run_cmd(["blaze", "clean", "--expunge"])


def blaze_build(js_readable_dirs, wasm_readable_dirs):
  """Blaze build everything in 1-go, for speed."""

  build_targets = [d + "/readable.js.zip" for d in js_readable_dirs]
  build_targets += [d + "/readable_wasm" for d in wasm_readable_dirs]
  if not args.nologs:
    build_targets += [d + ":readable_binary" for d in js_readable_dirs]

  cmd = ["blaze", "build", "-c", "fastbuild"] + build_targets
  return repo_util.run_cmd(cmd, include_stderr=True)


def replace_transpiled_wasm(readable_dirs):
  """Copy and reformat and replace with Blaze built WASM."""

  for readable_dir in readable_dirs:
    file_path = "blaze-bin/%s/readable_wasm.wat" % readable_dir
    output = readable_dir + "/output_wasm"
    output_file_path = output + "/module.wat.txt"

    repo_util.run_cmd(["mkdir", "-p", output])
    repo_util.run_cmd(["cp", file_path, output_file_path])
    # Temporary to keep readables unchanged.
    repo_util.run_cmd(["chmod", "-x", output_file_path])

    java_package = os.path.relpath(readable_dir, JAVA_DIR).replace("/", ".")
    _filter_wat_file(output_file_path, java_package)


def _filter_wat_file(wat_file_path, java_package):
  """Keep lines of code related to the readable examples."""

  filtered_lines = []
  # skip lines until we find a compilation unit from the example
  skip_line = True

  with open(wat_file_path, "r") as wat_file:
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

  with open(wat_file_path, "w") as wat_file:
    wat_file.writelines(filtered_lines)


def replace_transpiled_js(readable_dirs):
  """Copy and reformat and replace with Blaze built JS."""

  for readable_dir in readable_dirs:
    with tempfile.TemporaryDirectory() as tmpdirname:
      zip_file_path = "blaze-bin/%s/readable.js.zip" % readable_dir
      output = readable_dir + "/output_closure"

      # Clean the output directory from the result of last run.
      repo_util.run_cmd(["rm", "-Rf", output])
      repo_util.run_cmd(["mkdir", output])

      # Update the tmp directory with result of the new run.
      repo_util.run_cmd([
          "unzip", "-o", "-d", tmpdirname, zip_file_path, "-x", "*.java", "-x",
          "*.map"
      ])

      # Normalize the path relative to output directory.
      repo_util.run_cmd([
          "mv " + tmpdirname + "/" + os.path.relpath(readable_dir, JAVA_DIR) +
          "/* " + tmpdirname
      ], shell=True)

      # Move all files to => {file}.txt
      repo_util.run_cmd(
          ["find", "-type", "f", "-exec", "mv", "{}", "{}.txt", ";"],
          cwd=tmpdirname)

      # Move all the files to readable directory.
      repo_util.run_cmd(["mv " + tmpdirname + "/* " + output], shell=True)


def is_spam(line):
  """Whether the line is build output spam or not."""
  return re.match(r"\[[0-9,]+\s?/\s?[0-9,]+\]", line)


def gather_closure_warnings(build_log):
  """Gather Closure compiler warnings."""

  compiling_javascript_prefix = "From Compiling JavaScript for "
  build_logs = build_log.split("INFO: ")
  build_logs = [
      build_log[len(compiling_javascript_prefix):]
      for build_log in build_logs
      if build_log and build_log.startswith(compiling_javascript_prefix)
  ]

  if not build_logs:
    raise Exception("Did not find JSCompiler output.")

  for build_log in build_logs:
    # Remove unstable build timing lines.
    build_log = "\n".join(
        [line for line in build_log.splitlines() if not is_spam(line)])

    # Remove folder path spam.
    build_log = build_log.replace("blaze-out/k8-fastbuild/bin/", "")
    # Remove stable (but occasionally changing) line number details.
    build_log = replace_pattern(r"\:([0-9]*)\:([0-9]*)\:", "", build_log)
    build_log = replace_pattern(
        r"([0-9]+\|)",
        # replace the string "<number>|" with whitespace so that the error
        # message is still readable.
        lambda match: " " * len(match.group(0)),
        build_log)

    # Filter out the unstable ", ##% typed" message
    percent_typed_msg = (extract_pattern(r"g\(s\)(, .*? typed)", build_log))
    build_log = build_log.replace(percent_typed_msg, "")

    build_log_path = extract_pattern("//(.*?):",
                                     build_log) + "/build.closure.log"
    # Don't write build.log files for integration tests.
    if "readable/" not in build_log_path:
      continue
    if os.path.isfile(build_log_path):
      os.remove(build_log_path)

    if "\n0 error(s), 0 warning(s)" not in build_log:
      # There are errors, emit the build.log file.
      with open(build_log_path, "w") as build_log_file:
        build_log_file.write(build_log)


args = None


def main(argv):
  global args
  args = argv

  readable_name = args.readable_name[0]
  build_all = readable_name == "all"

  readable_pattern = ".*" if build_all else readable_name
  js_readable_dirs = get_readable_dirs(readable_pattern)
  wasm_readable_dirs = get_readable_dirs(readable_pattern, "_wasm")

  if not js_readable_dirs and not wasm_readable_dirs:
    print("No matching readables!")
    return -1

  print("Generating readable and build logs:")
  if not args.nologs:
    print("  Cleaning stale blaze outputs")
    blaze_clean()

  if build_all:
    print("  Blaze building everything")
  else:
    print("  Blaze building JS:")
    print("\n".join(["    " + d for d in js_readable_dirs or ["No matches"]]))
    print("  Blaze building WASM:")
    print("\n".join(["    " + d for d in wasm_readable_dirs or ["No matches"]]))

  build_log = blaze_build(js_readable_dirs, wasm_readable_dirs)

  if args.nologs:
    print("  Skipping logs!!!")
  else:
    print("  Processing build logs")
    gather_closure_warnings(build_log)

  print("  Copying and reformatting transpiled JS")
  replace_transpiled_js(js_readable_dirs)

  print("  Copying and reformatting transpiled WASM")
  replace_transpiled_wasm(wasm_readable_dirs)

  print("Check for changes in the readable examples")


def add_arguments(parser):
  parser.add_argument(
      "--nologs",
      action="store_true",
      help="omit the generations of build.logs")
  parser.add_argument(
      "readable_name",
      nargs=1,
      metavar="<name>",
      help="readable name (or 'all' for everything)")


def run_for_presubmit():
  argv = argparse.Namespace(readable_name=["all"], nologs=False)
  main(argv)
