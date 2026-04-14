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
"""Regenerates readables."""

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import repo_util


READABLE_TARGET_PATTERN = "transpiler/javatests/com/google/j2cl/readable/..."


def get_readables(name_filter, readable_kind, output_postfix=None):
  """Finds and returns the dirs of readable examples."""
  golden = f"readable_{readable_kind}_golden"
  output = f"output_{output_postfix or readable_kind}"
  return [
      {"dir": dir, "golden": golden, "output": output}
      for dir in _get_dirs_from_blaze_query(f"{name_filter}:{golden}$")
  ]


def _get_dirs_from_blaze_query(rules_filter):
  dirs = repo_util.run_cmd([
      "blaze", "query",
      f"filter('{rules_filter}', {READABLE_TARGET_PATTERN})",
      "--output=package"
  ]).splitlines()
  return list(filter(bool, dirs))


def blaze_test(readables):
  """Runs everything in 1-go, for speed and return the list of failures."""

  target_to_readables = {
      f"//{readable['dir']}:{readable['golden']}_test": readable
      for readable in readables
  }

  # Create a temporary file to store the Build Event Protocol output.
  with tempfile.NamedTemporaryFile() as bep_file:
    bep_file_path = bep_file.name
    cmd = [
        "blaze",
        "test",
        "--keep_going",
        f"--build_event_json_file={bep_file_path}",
    ] + list(target_to_readables.keys())
    result = subprocess.run(cmd, check=False, capture_output=True, text=True)
    if not os.path.exists(bep_file_path):
      print("Error invoking blaze!")
      print(result.stderr)
      raise FileNotFoundError("BEP file not generated! See the error output.")

    match = re.search(
        r"Streaming build results to: (https?://sponge2/\S+)", result.stderr
    )
    if not match: raise RuntimeError("Sponge link not found.")
    sponge_link = match.group(1)

    failed_targets = _process_blaze_results(bep_file_path, sponge_link)

  return [target_to_readables[t] for t in failed_targets]


def _process_blaze_results(bep_file_path, sponge_link):
  """Processes the Build Event Protocol file to find failed targets."""
  failed_targets = []
  build_finished = False
  with open(bep_file_path, "r") as f:
    for line in f:
      event = json.loads(line)
      event_id = event["id"]

      if "buildFinished" in event_id:
        build_finished = True

      if "testSummary" in event_id:
        label = event_id["testSummary"]["label"]
        status = event["testSummary"]["overallStatus"]
        if status != "PASSED":
          failed_targets.append(label)

      if "targetSummary" in event_id:
        label = event_id["targetSummary"]["label"]
        status = event["targetSummary"].get("overallBuildSuccess", False)
        if not status:
          print(f"Build failed for at least one target: {label}")
          print(f"Sponge link: {sponge_link}")
          sys.exit(1)

  if not build_finished: raise RuntimeError("Build finished event not found.")

  return failed_targets


def _replace_readable_outputs(readables):
  """Copy and replace readable directories with output from Blaze."""
  for readable in readables:
    transpiler_output = f"blaze-bin/{readable['dir']}/{readable['golden']}"
    output = f"{readable['dir']}/{readable['output']}"
    repo_util.run_cmd(["rm", "-Rf", output])
    repo_util.run_cmd(["mkdir", output])
    repo_util.run_cmd(
        [f"cp --no-preserve=mode -r {transpiler_output}/* {output}"],
        shell=True)


args = None


def main(argv):
  global args
  args = argv

  readable_name = args.readable_name[0]
  build_all = readable_name == "all"

  readable_pattern = ".*" if build_all else readable_name
  js_readables = (
      get_readables(readable_pattern, "closure")
      if "CLOSURE" in args.platforms
      else []
  )
  wasm_readables = (
      get_readables(readable_pattern, "wasm")
      if "WASM" in args.platforms
      else []
  )
  j2kt_readables = (
      get_readables(readable_pattern, "j2kt-jvm", "kt")
      if "J2KT" in args.platforms
      else []
  )

  j2kt_web_readables = (
      get_readables(readable_pattern, "j2kt-web", "j2kt_web")
      if "CLOSURE" in args.platforms
      else []
  )

  all_readables = (
      js_readables + wasm_readables + j2kt_readables + j2kt_web_readables
  )

  if not all_readables:
    print("No matching readables!")
    return -1

  print("Generating readables:")
  if build_all:
    print("  Blaze building everything")
  else:
    print("  Blaze building JS:")
    _print_readables(js_readables)
    print("  Blaze building JS from J2KT:")
    _print_readables(j2kt_web_readables)
    print("  Blaze building Wasm:")
    _print_readables(wasm_readables)
    print("  Blaze building J2KT:")
    _print_readables(j2kt_readables)

  stale_readables = blaze_test(all_readables)
  print("  Number of stale readables: %d" % len(stale_readables))
  if stale_readables:
    print("    Refreshing readables...")
    _replace_readable_outputs(stale_readables)


def _print_readables(readables):
  if not readables:
    print("    No matches")
  else:
    print("\n".join([f"    {d['dir']}" for d in readables]))


def add_arguments(parser):
  parser.add_argument(
      "readable_name",
      nargs=1,
      metavar="<name>",
      help="readable name (or 'all' for everything)")


def run_for_presubmit(argv):
  argv = argparse.Namespace(readable_name=["all"], platforms=argv.platforms)
  main(argv)
