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
import repo_util


READABLE_TARGET_PATTERN = "transpiler/javatests/com/google/j2cl/readable/..."


def get_readable_dirs(name_filter, rule_suffix=""):
  """Finds and returns the dirs of readable examples."""
  return _get_dirs_from_blaze_query(f"{name_filter}:readable{rule_suffix}$")


def _get_dirs_from_blaze_query(rules_filter):
  dirs = repo_util.run_cmd([
      "blaze", "query",
      f"filter('{rules_filter}', {READABLE_TARGET_PATTERN})",
      "--output=package"
  ]).splitlines()
  return list(filter(bool, dirs))


def blaze_build(
    js_readable_dirs,
    wasm_readable_dirs,
    j2kt_readable_dirs,
    j2kt_web_readable_dirs,
):
  """Blaze build everything in 1-go, for speed."""

  build_targets = [d + ":readable_golden" for d in js_readable_dirs]
  build_targets += [d + ":readable_wasm_golden" for d in wasm_readable_dirs]
  build_targets += [d + ":readable_j2kt_golden" for d in j2kt_readable_dirs]
  build_targets += [
      d + ":readable-j2kt-web_golden" for d in j2kt_web_readable_dirs
  ]

  cmd = ["blaze", "build", "-c", "fastbuild"] + build_targets
  return repo_util.run_cmd(cmd)


def replace_transpiled_wasm(readable_dirs):
  """Copy and replace with Blaze built Wasm."""
  _replace_readable_outputs(
      readable_dirs, "readable_wasm_golden", "output_wasm"
  )


def replace_transpiled_js(readable_dirs):
  """Copy and replace with Blaze built JS."""
  _replace_readable_outputs(readable_dirs, "readable_golden", "output_closure")


def replace_transpiled_j2kt_web(readable_dirs):
  """Copy and replace with Blaze built JS."""
  _replace_readable_outputs(
      readable_dirs, "readable-j2kt-web_golden", "output_j2kt_web"
  )


def replace_transpiled_j2kt(readable_dirs):
  """Copy and replace with Blaze built kt."""
  _replace_readable_outputs(readable_dirs, "readable_j2kt_golden", "output_kt")


def _replace_readable_outputs(readable_dirs, tree_artifact_dir, output_dir):
  """Copy and replace readable directories with output from Blaze."""
  for readable_dir in readable_dirs:
    transpiler_output = f"blaze-bin/{readable_dir}/{tree_artifact_dir}"
    output = f"{readable_dir}/{output_dir}"
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
  js_readable_dirs = get_readable_dirs(
      readable_pattern, "_js") if "CLOSURE" in args.platforms else []
  wasm_readable_dirs = get_readable_dirs(
      readable_pattern, "_wasm") if "WASM" in args.platforms else []
  j2kt_readable_dirs = (
      get_readable_dirs(readable_pattern, "-j2kt-jvm")
      if "J2KT" in args.platforms
      else []
  )

  j2kt_web_readable_dirs = (
      get_readable_dirs(readable_pattern, "-j2kt-web")
      if "CLOSURE" in args.platforms
      else []
  )

  if (
      not js_readable_dirs
      and not wasm_readable_dirs
      and not j2kt_readable_dirs
      and not j2kt_web_readable_dirs
  ):
    print("No matching readables!")
    return -1

  print("Generating readables:")
  if build_all:
    print("  Blaze building everything")
  else:
    print("  Blaze building JS:")
    print("\n".join(["    " + d for d in js_readable_dirs or ["No matches"]]))
    print("  Blaze building JS from J2KT:")
    print(
        "\n".join(
            ["    " + d for d in j2kt_web_readable_dirs or ["No matches"]]
        )
    )
    print("  Blaze building Wasm:")
    print("\n".join(["    " + d for d in wasm_readable_dirs or ["No matches"]]))
    print("  Blaze building J2KT:")
    print("\n".join(["    " + d for d in j2kt_readable_dirs or ["No matches"]]))

  blaze_build(
      js_readable_dirs,
      wasm_readable_dirs,
      j2kt_readable_dirs,
      j2kt_web_readable_dirs,
  )

  if js_readable_dirs:
    print("  Copying and reformatting transpiled JS")
    replace_transpiled_js(js_readable_dirs)

  if j2kt_web_readable_dirs:
    print("  Copying and reformatting transpiled JS from J2KT")
    replace_transpiled_j2kt_web(j2kt_web_readable_dirs)

  if wasm_readable_dirs:
    print("  Copying and reformatting transpiled Wasm")
    replace_transpiled_wasm(wasm_readable_dirs)

  if j2kt_readable_dirs:
    print("  Copying and reformatting transpiled KT")
    replace_transpiled_j2kt(j2kt_readable_dirs)

  print("Check for changes in the readable examples")


def add_arguments(parser):
  parser.add_argument(
      "readable_name",
      nargs=1,
      metavar="<name>",
      help="readable name (or 'all' for everything)")


def run_for_presubmit(argv):
  argv = argparse.Namespace(readable_name=["all"], platforms=argv.platforms)
  main(argv)
