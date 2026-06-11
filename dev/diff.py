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
"""Diffs optimized integration test JS with current CL changes."""

# pylint: disable=missing-function-docstring

import argparse
import math
import os
import re
import shutil
import subprocess
import sys
import tempfile

import repo_util


class TargetInfo:

  def __init__(self, workspace_name, workspace_path, blaze_target):
    self.workspace_name = workspace_name
    self.workspace_path = workspace_path
    self.blaze_target = blaze_target

  def __str__(self):
    return (
        f"{self.workspace_name}@{self.blaze_target}"
        if self.workspace_name
        else self.blaze_target
    )

  def get_output_file(self):
    file_path = "blaze-bin/" + repo_util.get_file_from_target(self.blaze_target)
    if self.workspace_path:
      return f"{self.workspace_path}/{file_path}"
    else:
      return file_path

  def get_formatted_file(self):
    return "/tmp/" + self.get_output_file().replace("/", ".")

  def to_j2cl_size_target(self):
    target_info = _ = TargetInfo(
        "j2cl-size", repo_util.get_repo_path("j2cl-size"), self.blaze_target
    )
    # sync the j2cl-size workspace to the same base cl
    repo_util.sync_j2size_repo()
    return target_info

  def is_wasm(self):
    return self.blaze_target.endswith(".wasm")


def _create_target_info(target):
  decomposed_target = target.split("@", 1)
  if len(decomposed_target) == 2:
    workspace_name, blaze_target = decomposed_target
    workspace_path = repo_util.get_repo_path(workspace_name)
    if not os.path.isdir(workspace_path):
      raise argparse.ArgumentTypeError(f"No such workspace {workspace_name}")
  else:
    workspace_name = None
    workspace_path = None
    blaze_target = target

  if blaze_target.startswith("//"):
    # remove the '//'
    blaze_target = blaze_target[2:]
  else:
    # Check for size_report format: (test)/(java|kotlin).(variant)
    match = re.fullmatch(r"(\w+)/(java|kotlin)\.(.+)", blaze_target)
    if match:
      test, lang, variant = match.groups()
      blaze_target = f"{lang}/{test}.{variant}"

    # otherwise assume it is an integration test name.
    # Format: (java|kotlin)/(test).(variant)
    blaze_target = repo_util.get_optimized_target(blaze_target)

  blaze_target = _get_artifact_target(blaze_target, workspace_path)
  return TargetInfo(workspace_name, workspace_path, blaze_target)


def _get_artifact_target(blaze_target, workspace_path):
  rule_kind = repo_util.get_rule_kind(blaze_target, workspace_path)
  if rule_kind == "js_binary":
    return blaze_target + ".js"
  elif rule_kind == "_j2wasm_application":
    return blaze_target + ".wasm"
  elif rule_kind == "_size_report":
    # Size report targets doesn't need extension.
    return blaze_target
  else:
    raise argparse.ArgumentTypeError(f"Unknown target kind {rule_kind}")


def main(argv):
  if argv.bisect_size:
    if len(argv.targets) != 1:
      raise argparse.ArgumentTypeError(
          "--bisect-size requires exactly one target"
      )
    _bisect(argv.targets[0], argv.bisect_size)
    return

  if len(argv.targets) > 2:
    raise argparse.ArgumentTypeError("More than 2 targets to compare.")

  original = _create_target_info(argv.targets[0])

  if len(argv.targets) == 1:
    if original.workspace_path:
      raise argparse.ArgumentTypeError(
          "Workspace is not allowed on a single target compare."
      )
    # We are diffing a change against the head version. Reuse the 'j2cl-size'
    # repo for building the head version of the target.
    modified = original
    original = modified.to_j2cl_size_target()
  else:
    modified = _create_target_info(argv.targets[1])

  _diff(original, modified, argv.size, argv.group_by, argv.filter_noise)


def _diff(original, modified, is_size, group_by, filter_noise):
  print(f"Constructing diff for '{modified}'.")
  if (
      original.workspace_name != "j2cl-size"
      or original.blaze_target != modified.blaze_target
  ):
    print(f"    against '{original}'.")

  original_targets = [original.blaze_target]
  modified_targets = [modified.blaze_target]
  if original.is_wasm():
    original_targets += [original.blaze_target + ".map"]
    modified_targets += [modified.blaze_target + ".map"]

  print("  Building targets.")
  repo_util.build_targets_with_workspace(
      original_targets,
      modified_targets,
      original.workspace_path,
      modified.workspace_path,
      [] if is_size else ["--define=J2CL_APP_STYLE=PRETTY"],
  )

  if is_size:
    _diff_size(original, modified, group_by)
    return

  if original.is_wasm():
    print("  Disassembling.")
    repo_util.build(["//third_party/binaryen:wasm-dis"])
    wasm_dis_cmd = [
        "blaze-bin/third_party/binaryen/src/wasm-dis",
        "--enable-gc",
    ]
    repo_util.run_cmd(
        wasm_dis_cmd
        + [
            original.get_output_file(),
            "--source-map",
            original.get_output_file() + ".map",
            "-o",
            original.get_formatted_file(),
        ]
    )
    repo_util.run_cmd(
        wasm_dis_cmd
        + [
            modified.get_output_file(),
            "--source-map",
            modified.get_output_file() + ".map",
            "-o",
            modified.get_formatted_file(),
        ]
    )
  else:
    print("  Formatting.")
    shutil.copyfile(original.get_output_file(), original.get_formatted_file())
    shutil.copyfile(modified.get_output_file(), modified.get_formatted_file())
    repo_util.run_cmd([
        "clang-format",
        "-i",
        original.get_formatted_file(),
        modified.get_formatted_file(),
    ])

  if filter_noise:
    print("  Reducing noise.")
    # Replace the numeric part of the variable id generation from JsCompiler
    # or binaryen to reduce noise in the final diff.
    # The patterns we want to match are:
    #   $jscomp$1234, $jscomp$inline_1234, JSC$1234,
    #   type $1234, call_ref $1234, ref $1234
    # The numeric part of these patterns will be replaced by the character '#'
    repo_util.run_cmd([
        "sed",
        "-i",
        "-E",
        r"s/(\$(jscomp|gimport)\$(inline_)?|JSC\$|((type|call_ref|ref)\ \$))[0-9]+/\1#/g",
        original.get_formatted_file(),
        modified.get_formatted_file(),
    ])

  print("  Starting diff...")
  subprocess.call(
      "${P4DIFF:-diff} %s %s"
      % (original.get_formatted_file(), modified.get_formatted_file()),
      shell=True,
  )


def _diff_size(original, modified, group_by):
  """Calculates and outputs the size difference of the original and modified targets."""
  print("  Performing size diff...")

  _print_size_diff(
      "Uncompressed",
      os.path.getsize(original.get_output_file()),
      os.path.getsize(modified.get_output_file())
  )

  _print_size_diff(
      "Compressed",
      repo_util.get_compressed_size(original.get_output_file()),
      repo_util.get_compressed_size(modified.get_output_file()),
  )

  if original.is_wasm():
    _run_bloaty(original, modified, group_by)


def _run_bloaty(original, modified, group_by):
  """Runs go/bloaty to analyze the size difference of the original and modified targets."""
  with tempfile.NamedTemporaryFile(suffix=".bloaty", mode="w+t") as config:
    # Create a custom data source for bloaty to parse packages
    config.write(r"""custom_data_source: {
      name: "package"
      base_data_source: "compileunits"
      # Names are in the form of <google3-package>/<target>.js/<file>
      # Extracting text before /<target>.js give us the google3 package name.
      rewrite: {
        pattern: "^(.*)/.*\\.js/"
        replacement: "\\1"
      }
    }
    """)
    config.flush()

    bloaty_domain_by_group = {
        "file": "compileunits",
        "line": "inlines",
        "package": "package",
    }
    subprocess.call([
        "/google/bin/releases/protobuf-team/bloaty/bloaty",
        "--allow_unsafe_non_google3_input",
        "--domain=file",
        "-c", config.name,
        "-d", "sections," + bloaty_domain_by_group[group_by],
        "-n", "0",  # No limit on number of rows
        "--source-map="
        + f"{original.get_output_file()}={original.get_output_file()}.map",
        "--source-map="
        + f"{modified.get_output_file()}={modified.get_output_file()}.map",
        f"{modified.get_output_file()}",
        "--",
        f"{original.get_output_file()}",
    ])


def _print_size_diff(prefix, original_size, modified_size):
  diff = modified_size - original_size
  print(f"    {prefix}")
  print(f"      Original: {original_size}")
  print(f"      Modified: {modified_size}")
  print(f"      Diff: {diff:+} ({diff/original_size:.1%})")


def _bisect(target_str, cl_range):
  start_cl, end_cl = cl_range
  print(f"Bisecting size change for '{target_str}'")
  print(f"    in range  {start_cl}..{end_cl}")

  j2cl_size_target = _create_target_info(target_str).to_j2cl_size_target()

  # Verify initial and final states
  print(f"Checking initial state at {start_cl}...")
  size_start = _measure_size(j2cl_size_target, start_cl)
  print(f"Size at {start_cl}: {size_start}")

  print(f"Checking final state at {end_cl}...")
  size_end = _measure_size(j2cl_size_target, end_cl)
  print(f"Size at {end_cl}: {size_end}")

  if size_start == size_end:
    print("Start and end sizes are the same. Cannot bisect.")
    return

  low = start_cl
  high = end_cl
  max_steps = math.ceil(math.log2(high - low + 1))
  step_count = 0

  while low < high:
    step_count += 1
    mid = (low + high) // 2
    print(f"[Step {step_count}/{max_steps}] Checking CL {mid}...")
    size = _measure_size(j2cl_size_target, mid)
    print(f"  Size at {mid}: {size}")

    if size == size_start:
      low = mid + 1
    else:
      high = mid

  culprit_cl = low
  print(f"Culprit CL identified: cl/{culprit_cl}")
  print("--------------------------------------------------")
  sys.stdout.flush()
  subprocess.call(f"g4 describe -s {culprit_cl}", shell=True)
  print(f"Link to culprit CL: http://cl/{culprit_cl}")


def _measure_size(target_info, cl):
  repo_util.sync_jsize_repo_to_cl(cl)
  repo_util.build([target_info.blaze_target], cwd=target_info.workspace_path)
  return os.path.getsize(target_info.get_output_file())


def _parse_cl_range(cl_range_str):
  """Parses a CL range string of the format 'start-end' or 'auto'."""
  if cl_range_str == "auto":
    print("Auto-calculating CL range...")
    start_cl = repo_util.get_last_cl_for_size_report()
    end_cl = repo_util.get_current_cl()
  else:
    match = re.fullmatch(r"(\d+)-(\d+)", cl_range_str)
    if not match:
      print(f"Invalid CL range: {cl_range_str}")
      raise argparse.ArgumentTypeError(
          f"Invalid CL range: {cl_range_str}. Expected format: start-end or"
          " 'auto'"
      )
    start_cl, end_cl = map(int, match.groups())

  if start_cl >= end_cl:
    print(f"Start CL ({start_cl}) must be smaller than end CL ({end_cl}).")
    if cl_range_str == "auto":
      print("Your workspace might be out of date. Please sync.")
    sys.exit(1)
  return start_cl, end_cl


def add_arguments(parser):
  parser.add_argument(
      "--size",
      default=False,
      action=argparse.BooleanOptionalAction,
      help="Perform a size diff instead of a binary diff.",
  )

  parser.add_argument(
      "--group_by",
      default="file",
      choices=["line", "file", "package"],
      help="For --size, group by line, file or target. Defaults to file.",
  )

  parser.add_argument(
      "--filter_noise",
      default=True,
      action=argparse.BooleanOptionalAction,
      help="Filter noise in the diff due to difference in variable indexes.",
  )

  parser.add_argument(
      "--bisect-size",
      metavar="RANGE",
      type=_parse_cl_range,
      help=(
          "Bisect size change in CL range (e.g. 12345-67890) "
          "or 'auto' to auto-calculate."
      ),
  )

  parser.add_argument(
      "targets",
      nargs="+",
      metavar="<target>",
      help=(
          "Targets that need to be compared. Target must be in the format:"
          " ({workspace}@)?{target_label} with workspace: the name of your"
          " piper client. Optional. target_label: can be a full blaze target"
          " label (starting with //) or an integration test name with the"
          " format: (java|kotlin)/{integration_test_name}.{variant}"
      ),
  )
