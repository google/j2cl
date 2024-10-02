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

import argparse
import os
import shutil
import subprocess

import repo_util


class TargetInfo:
  workspace_path = None
  blaze_target = ""

  def __init__(self, workspace_path, blaze_target):
    self.workspace_path = workspace_path
    self.blaze_target = blaze_target

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
        repo_util.get_repo_path("j2cl-size"), self.blaze_target
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
    workspace_path = None
    blaze_target = target

  if blaze_target.startswith("//"):
    # remove the '//'
    blaze_target = blaze_target[2:]
  else:
    # This is an integration test name. Format: (java|kotlin)/(test)(.version)?
    blaze_target = repo_util.get_optimized_target(blaze_target)

  rule_kind = repo_util.get_rule_kind(blaze_target, workspace_path)
  if rule_kind == "js_binary":
    blaze_target += ".js"
  if rule_kind == "_j2wasm_application":
    blaze_target += ".wasm"
  elif rule_kind == "_size_report":
    # Size report targets doesn't need extension.
    pass
  elif rule_kind:
    raise argparse.ArgumentTypeError(f"Unknown target kind {rule_kind}")
  else:
    raise argparse.ArgumentTypeError(f"No such target {blaze_target}")

  return TargetInfo(workspace_path, blaze_target)


def main(argv):
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

  _diff(original, modified, argv.size, argv.lines, argv.filter_noise)


def _diff(original, modified, is_size, is_size_lines, filter_noise):
  print(f"Constructing a diff of changes in '{modified.blaze_target}'.")

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
      ["--define=J2CL_APP_STYLE=PRETTY"],
  )

  if is_size:
    _diff_size(original, modified, is_size_lines)
    return

  if original.is_wasm():
    print("  Disassembling.")
    repo_util.build(["//third_party/binaryen:wasm-dis"])
    wasm_dis_cmd = ["blaze-bin/third_party/binaryen/wasm-dis", "--enable-gc"]
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
        r"s/(\$jscomp\$(inline_)?|JSC\$|((type|call_ref|ref)\ \$))[0-9]+/\1#/g",
        original.get_formatted_file(),
        modified.get_formatted_file(),
    ])

  print("  Starting diff...")
  subprocess.call(
      "${P4DIFF:-diff} %s %s"
      % (original.get_formatted_file(), modified.get_formatted_file()),
      shell=True,
  )


def _diff_size(original, modified, is_size_lines):
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
    # go/bloaty
    subprocess.call(
        [
            "/google/bin/releases/protobuf-team/bloaty/bloaty",
            "--allow_unsafe_non_google3_input",
            "--domain=file",
            "-d", "inlines" if is_size_lines else "compileunits",
            "-n", "0",  # No limit on number of rows
            "--source-map="
            + f"{original.get_output_file()}={original.get_output_file()}.map",
            "--source-map="
            + f"{modified.get_output_file()}={modified.get_output_file()}.map",
            f"{modified.get_output_file()}",
            "--",
            f"{original.get_output_file()}",
        ]
    )


def _print_size_diff(prefix, original_size, modified_size):
  diff = modified_size - original_size
  print(f"    {prefix}")
  print(f"      Original: {original_size}")
  print(f"      Modified: {modified_size}")
  print(f"      Diff: {diff:+} ({diff/original_size:.1%})")


def add_arguments(parser):
  parser.add_argument(
      "--size",
      default=False,
      action=argparse.BooleanOptionalAction,
      help="Perform a size diff instead of a binary diff.",
  )

  parser.add_argument(
      "--lines",
      default=False,
      action=argparse.BooleanOptionalAction,
      help="For the size diff, compare by line. Defaults to file. See --size.",
  )

  parser.add_argument(
      "--filter_noise",
      default=True,
      action=argparse.BooleanOptionalAction,
      help="Filter noise in the diff due to difference in variable indexes.",
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
