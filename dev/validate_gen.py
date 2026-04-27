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
"""Validates j2 gen functionality."""

# pylint: disable=missing-function-docstring

import io
import os
import shlex
import subprocess
import sys


_out = io.StringIO()


class ValidationTest:
  """Base class for validation tests."""

  def setup(self):
    # Clear the buffer
    _out.seek(0)
    _out.truncate(0)

  def teardown(self):
    pass

  def run_test(self, name, func):
    print(f"{name}: ", end="", flush=True)
    self.setup()
    try:
      func()
      print("\033[92mSUCCESS\033[0m")
      return True
    except BaseException as e:  # pylint: disable=broad-exception-caught
      print("\033[91mFAILED\033[0m")
      print("--- Error ---")
      print(str(e))
      print("--- Captured Output ---")
      print(_out.getvalue())
      return False
    finally:
      self.teardown()


READABLE_DIR = "transpiler/javatests/com/google/j2cl/readable/java/emptyclass"
READABLE_DIR_KT = "transpiler/javatests/com/google/j2cl/readable/kotlin/emptyclass"
GOLDEN_CLOSURE = os.path.join(
    READABLE_DIR, "output_closure", "EmptyClass.java.js.txt"
)
GOLDEN_WASM = os.path.join(READABLE_DIR, "output_wasm", "contents.wat.txt")
GOLDEN_KT = os.path.join(READABLE_DIR, "output_kt", "EmptyClass.kt.txt")
GOLDEN_KT_WEB = os.path.join(
    READABLE_DIR, "output_j2kt_web", "EmptyClass.java.js.txt"
)

SOURCE_FILE = os.path.join(READABLE_DIR, "EmptyClass.java")
BUILD_FILE = os.path.join(READABLE_DIR, "BUILD")


class GenValidationTest(ValidationTest):
  """Validation tests for j2 gen."""

  def setup(self):
    super().setup()

    self._backup = {
        GOLDEN_CLOSURE: open(GOLDEN_CLOSURE, "r").read(),
        GOLDEN_WASM: open(GOLDEN_WASM, "r").read(),
        GOLDEN_KT: open(GOLDEN_KT, "r").read(),
        GOLDEN_KT_WEB: open(GOLDEN_KT_WEB, "r").read(),
        SOURCE_FILE: open(SOURCE_FILE, "r").read(),
        BUILD_FILE: open(BUILD_FILE, "r").read(),
    }

    # Make sure we are starting with a clean state.
    self._ensure_clean_state()

  def teardown(self):
    super().teardown()
    for path, content in self._backup.items():
      with open(path, "w") as f:
        f.write(content)

  def _ensure_clean_state(self):
    if _out.getvalue():
      raise AssertionError("Output buffer not cleared between tests!")
    localout = io.StringIO()
    _j2("gen java/emptyclass", out_stream=localout)
    _assert_in("Number of stale readables: 0", localout.getvalue())

  def test_golden_file_updates(self):
    with open(GOLDEN_CLOSURE, "a") as f:
      f.write("\n// STALE COMMENT\n")
    with open(GOLDEN_WASM, "a") as f:
      f.write("\n;; STALE COMMENT\n")
    with open(GOLDEN_KT, "a") as f:
      f.write("\n// STALE COMMENT\n")
    with open(GOLDEN_KT_WEB, "a") as f:
      f.write("\n// STALE COMMENT\n")

    _j2("gen java/emptyclass")
    _assert_output("Number of stale readables: 4")

    with open(GOLDEN_CLOSURE, "r") as f:
      _assert_not_in("// STALE COMMENT", f.read())
    with open(GOLDEN_WASM, "r") as f:
      _assert_not_in(";; STALE COMMENT", f.read())
    with open(GOLDEN_KT, "r") as f:
      _assert_not_in("// STALE COMMENT", f.read())
    with open(GOLDEN_KT_WEB, "r") as f:
      _assert_not_in("// STALE COMMENT", f.read())

  def test_failed_compilation(self):
    with open(SOURCE_FILE, "w") as f:
      f.write("INVALID JAVA CODE")

    _j2_expecting_failure("gen java/emptyclass")
    _assert_output("No test status for targets")
    _assert_output("Sponge link:")

  def test_broken_build_file(self):
    with open(BUILD_FILE, "w") as f:
      f.write("INVALID BAZEL SYNTAX")

    _j2_expecting_failure("gen java/emptyclass")
    _assert_output("Error while running command")
    _assert_output("blaze query filter")

  def test_missing_file_in_srcs(self):
    original_build = self._backup[BUILD_FILE]
    _assert_in('glob(["*.java"])', original_build)

    broken_build = original_build.replace('glob(["*.java"])', "[]")
    with open(BUILD_FILE, "w") as f:
      f.write(broken_build)

    _j2_expecting_failure("gen java/emptyclass")
    _assert_output("No test status for targets")
    _assert_output("Sponge link:")

  def test_platform_filtering(self):
    _j2("-p CLOSURE gen java/emptyclass")
    _assert_output("Blaze building Wasm:\n    No matches")

  def test_pattern(self):
    _j2("-p CLOSURE gen empty.*")
    _assert_not_output("No matching readables!")
    _assert_output(READABLE_DIR)
    _assert_output(READABLE_DIR_KT)

  def test_pattern2(self):
    _j2("-p CLOSURE gen java/empty.*")
    _assert_not_output("No matching readables!")
    _assert_output(READABLE_DIR)
    _assert_not_output(READABLE_DIR_KT)

  def test_pattern3(self):
    _j2("-p CLOSURE gen kotlin/empty.*")
    _assert_not_output("No matching readables!")
    _assert_not_output(READABLE_DIR)
    _assert_output(READABLE_DIR_KT)

  def test_pattern_no_matches(self):
    _j2("-p CLOSURE gen empty")
    _assert_output("No matching readables!")


class DiffValidationTest(ValidationTest):
  """Validation tests for j2 diff."""


def _j2(args_str, out_stream=None):
  if out_stream is None:
    out_stream = _out
  args = shlex.split(args_str)
  cmd = ["python3", "dev/j2.py"] + args
  result = subprocess.run(cmd, capture_output=True, text=True, check=False)
  out_stream.write(result.stdout)
  out_stream.write(result.stderr)
  if result.returncode != 0:
    raise SystemExit(result.returncode)


def _j2_expecting_failure(args_str):
  try:
    _j2(args_str)
    raise AssertionError(f"j2 gen {args_str} expected to fail but didn't.")
  except SystemExit:
    return


def _assert_in(needle, haystack, msg=None):
  if needle not in haystack:
    raise AssertionError(msg or f"Expected to find '{needle}'")


def _assert_not_in(needle, haystack, msg=None):
  if needle in haystack:
    raise AssertionError(msg or f"Did not expect to find '{needle}'.")


def _assert_output(needle):
  _assert_in(needle, _out.getvalue())


def _assert_not_output(needle):
  _assert_not_in(needle, _out.getvalue())


def main(argv):
  print("Starting j2 validation...\n")

  test_suites_mapping = {
      "gen": GenValidationTest(),
      "diff": DiffValidationTest(),
  }
  test_suites = (
      [test_suites_mapping[argv.test_suite]]
      if argv.test_suite
      else test_suites_mapping.values()
  )

  success = True
  for test_suite in test_suites:
    for name in dir(test_suite):
      if not name.startswith("test_"):
        continue
      func = getattr(test_suite, name)
      display_name = name.replace("_", " ").capitalize()
      success &= test_suite.run_test(display_name, func)

  print("")
  if not success:
    sys.exit(1)


def add_arguments(parser):
  parser.add_argument(
      "test_suite",
      nargs="?",
      choices=["gen", "diff"],
      help="Test suite to run (gen or diff). If omitted, all tests are run.",
  )
