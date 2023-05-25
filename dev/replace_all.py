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
import repo_util


READABLE_TARGET_PATTERN = "transpiler/javatests/com/google/j2cl/readable/..."


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


def blaze_build(
    js_readable_dirs,
    wasm_readable_dirs,
    wasm_modular_readable_dirs,
    wasm_imports_readable_dirs,
    kt_readable_dirs,
):
  """Blaze build everything in 1-go, for speed."""

  build_targets = [d + ":readable_golden" for d in js_readable_dirs]
  build_targets += [d + ":readable_wasm_golden" for d in wasm_readable_dirs]
  build_targets += [d + ":readable_wasm_imports_golden"
                    for d in wasm_imports_readable_dirs]
  build_targets += [
      d + ":readable_wasm_modular_golden" for d in wasm_modular_readable_dirs
  ]
  build_targets += [d + ":readable_j2kt_golden" for d in kt_readable_dirs]
  if not args.nologs:
    build_targets += [d + ":readable_binary" for d in js_readable_dirs]

  cmd = ["blaze", "build", "-c", "fastbuild"] + build_targets
  return repo_util.run_cmd(cmd, include_stderr=True)


def replace_transpiled_wasm(readable_dirs):
  """Copy and replace with Blaze built Wasm."""
  _replace_readable_outputs(
      readable_dirs, "readable_wasm_golden", "output_wasm"
  )


def replace_transpiled_wasm_modular(readable_dirs):
  """Copy and replace with Blaze built Wasm modular output."""
  _replace_readable_outputs(
      readable_dirs,
      "readable_wasm_modular_golden",
      "output_wasm_modular",
  )


def replace_transpiled_wasm_imports(readable_dirs):
  """Copy and replace with Blaze built Wasm imports."""
  _replace_readable_outputs(
      readable_dirs, "readable_wasm_imports_golden", "output_wasm_imports"
  )


def replace_transpiled_js(readable_dirs):
  """Copy and replace with Blaze built JS."""
  _replace_readable_outputs(readable_dirs, "readable_golden", "output_closure")


def is_spam(line):
  """Whether the line is build output spam or not."""
  return re.match(r"\[[0-9,]+\s?/\s?[0-9,]+\]", line)


def gather_closure_warnings(build_log):
  """Gather Closure compiler warnings."""

  compiling_javascript_prefix = "From Compiling JavaScript for checks "
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


def replace_transpiled_j2kt(readable_dirs):
  """Copy and replace with Blaze built kt."""
  _replace_readable_outputs(readable_dirs, "readable_j2kt_golden", "output_kt")


def _replace_readable_outputs(readable_dirs, tree_artifact_dir, output_dir):
  """Copy and replace readable directories with output from Blaze."""
  for readable_dir in readable_dirs:
    transpiler_output = "blaze-bin/%s/%s" % (readable_dir, tree_artifact_dir)
    output = "%s/%s" % (readable_dir, output_dir)
    repo_util.run_cmd(["rm", "-Rf", output])
    repo_util.run_cmd(["mkdir", output])
    repo_util.run_cmd(
        ["cp --no-preserve=mode -r %s/* %s" % (transpiler_output, output)],
        shell=True)


args = None


def main(argv):
  global args
  args = argv

  readable_name = args.readable_name[0]
  build_all = readable_name == "all"

  readable_pattern = ".*" if build_all else readable_name
  js_readable_dirs = get_readable_dirs(
      readable_pattern, "_js") if "CLOSURE" in args.platforms else []
  wasm_readable_dirs = get_readable_dirs(
      readable_pattern, "_wasm") if "WASM" in args.platforms else []
  wasm_modular_readable_dirs = (
      get_readable_dirs(readable_pattern, "_wasm_modular_golden")
      if "WASM" in args.platforms
      else []
  )
  wasm_imports_readable_dirs = (
      get_readable_dirs(readable_pattern, "_wasm_imports_golden")
      if "WASM" in args.platforms else [])
  kt_readable_dirs = get_readable_dirs(
      readable_pattern, "-j2kt-jvm") if "J2KT" in args.platforms else []

  if not js_readable_dirs and not wasm_readable_dirs and not kt_readable_dirs:
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
    print("  Blaze building Wasm:")
    print("\n".join(["    " + d for d in wasm_readable_dirs or ["No matches"]]))
    print("  Blaze building J2KT:")
    print("\n".join(["    " + d for d in kt_readable_dirs or ["No matches"]]))

  build_log = blaze_build(
      js_readable_dirs,
      wasm_readable_dirs,
      wasm_modular_readable_dirs,
      wasm_imports_readable_dirs,
      kt_readable_dirs,
  )

  if js_readable_dirs:
    if args.nologs:
      print("  Skipping logs!!!")
    else:
      print("  Processing build logs")
      gather_closure_warnings(build_log)
    print("  Copying and reformatting transpiled JS")
    replace_transpiled_js(js_readable_dirs)

  if wasm_readable_dirs:
    print("  Copying and reformatting transpiled Wasm")
    replace_transpiled_wasm(wasm_readable_dirs)

  if wasm_modular_readable_dirs:
    print("  Copying and reformatting transpiled Wasm modular")
    replace_transpiled_wasm_modular(wasm_modular_readable_dirs)

  if wasm_imports_readable_dirs:
    print("  Copying and reformatting transpiled Wasm imports")
    replace_transpiled_wasm_imports(wasm_imports_readable_dirs)

  if kt_readable_dirs:
    print("  Copying and reformatting transpiled KT")
    replace_transpiled_j2kt(kt_readable_dirs)

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


def run_for_presubmit(argv):
  argv = argparse.Namespace(
      readable_name=["all"], nologs=False, platforms=argv.platforms)
  main(argv)
