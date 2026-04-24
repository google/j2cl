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

import argparse
import contextlib
import io
import os
import sys
import replace_all

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


_out = io.StringIO()
_backup = {}


def _setup():
  # Clear the buffer
  _out.seek(0)
  _out.truncate(0)

  _backup[GOLDEN_CLOSURE] = open(GOLDEN_CLOSURE, "r").read()
  _backup[GOLDEN_WASM] = open(GOLDEN_WASM, "r").read()
  _backup[GOLDEN_KT] = open(GOLDEN_KT, "r").read()
  _backup[GOLDEN_KT_WEB] = open(GOLDEN_KT_WEB, "r").read()
  _backup[SOURCE_FILE] = open(SOURCE_FILE, "r").read()
  _backup[BUILD_FILE] = open(BUILD_FILE, "r").read()

  # Make sure we are starting with a clean state.
  _ensure_clean_state()


def _teardown():
  for path, content in _backup.items():
    with open(path, "w") as f:
      f.write(content)


def _ensure_clean_state():
  if _out.getvalue():
    raise AssertionError("Output buffer not cleared between tests!")
  localout = io.StringIO()
  with contextlib.redirect_stdout(localout):
    _run_gen()
  _assert_in("Number of stale readables: 0", localout.getvalue())


def _assert_in(needle, haystack, msg=None):
  if needle not in haystack:
    raise AssertionError(msg or f"Expected to find '{needle}'")


def _assert_not_in(needle, haystack, msg=None):
  if needle in haystack:
    raise AssertionError(msg or f"Did not expect to find '{needle}'.")


def _fail():
  raise AssertionError("j2 gen expected to fail but didn't.")


def _run_gen(name="java/emptyclass", platforms=None):
  argv = argparse.Namespace(
      readable_name=[name], platforms=platforms or ["CLOSURE", "WASM", "J2KT"]
  )
  replace_all.main(argv)


def _run_gen_expecting_failure(**kwargs):
  try:
    _run_gen(**kwargs)
    _fail()
  except SystemExit:
    return


def test_golden_file_updates():
  with open(GOLDEN_CLOSURE, "a") as f:
    f.write("\n// STALE COMMENT\n")
  with open(GOLDEN_WASM, "a") as f:
    f.write("\n;; STALE COMMENT\n")
  with open(GOLDEN_KT, "a") as f:
    f.write("\n// STALE COMMENT\n")
  with open(GOLDEN_KT_WEB, "a") as f:
    f.write("\n// STALE COMMENT\n")

  _run_gen()
  output = _out.getvalue()

  _assert_in("Number of stale readables: 4", output)

  with open(GOLDEN_CLOSURE, "r") as f:
    new_closure = f.read()
  with open(GOLDEN_WASM, "r") as f:
    new_wasm = f.read()
  with open(GOLDEN_KT, "r") as f:
    new_kt = f.read()
  with open(GOLDEN_KT_WEB, "r") as f:
    new_kt_web = f.read()

  _assert_not_in("// STALE COMMENT", new_closure, "Closure golden not updated!")
  _assert_not_in(";; STALE COMMENT", new_wasm, "WASM golden not updated!")
  _assert_not_in("// STALE COMMENT", new_kt, "KT golden not updated!")
  _assert_not_in("// STALE COMMENT", new_kt_web, "J2KT Web golden not updated!")


def test_failed_compilation():
  with open(SOURCE_FILE, "w") as f:
    f.write("INVALID JAVA CODE")

  _run_gen_expecting_failure()
  _assert_in("No test status for targets", _out.getvalue())
  _assert_in("Sponge link:", _out.getvalue())


def test_broken_build_file():
  with open(BUILD_FILE, "w") as f:
    f.write("INVALID BAZEL SYNTAX")

  _run_gen_expecting_failure()
  _assert_in("Error while running command", _out.getvalue())
  _assert_in("blaze query filter", _out.getvalue())


def test_missing_file_in_srcs():
  original_build = _backup[BUILD_FILE]
  _assert_in('glob(["*.java"])', original_build)

  broken_build = original_build.replace('glob(["*.java"])', "[]")
  with open(BUILD_FILE, "w") as f:
    f.write(broken_build)

  _run_gen_expecting_failure()
  _assert_in("No test status for targets", _out.getvalue())
  _assert_in("Sponge link:", _out.getvalue())


def test_platform_filtering():
  _run_gen(platforms=["CLOSURE"])
  _assert_in("Blaze building Wasm:\n    No matches", _out.getvalue())


def test_pattern():
  _run_gen(name="empty.*", platforms=["CLOSURE"])
  output = _out.getvalue()
  _assert_not_in("No matching readables!", output)
  _assert_in(READABLE_DIR, output)
  _assert_in(READABLE_DIR_KT, output)


def test_pattern2():
  _run_gen(name="java/empty.*", platforms=["CLOSURE"])
  output = _out.getvalue()
  _assert_not_in("No matching readables!", output)
  _assert_in(READABLE_DIR, output)
  _assert_not_in(READABLE_DIR_KT, output)


def test_pattern3():
  _run_gen(name="kotlin/empty.*", platforms=["CLOSURE"])
  output = _out.getvalue()
  _assert_not_in("No matching readables!", output)
  _assert_not_in(READABLE_DIR, output)
  _assert_in(READABLE_DIR_KT, output)


def test_pattern_no_matches():
  _run_gen(name="empty", platforms=["CLOSURE"])
  output = _out.getvalue()
  _assert_in("No matching readables!", output)


def _run_test(name, func):
  print(f"{name}: ", end="", flush=True)
  _setup()
  try:
    with contextlib.redirect_stdout(_out):
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
    _teardown()


def main(unused_argv):
  print("Starting j2 gen validation...\n")

  success = True
  for name, func in list(globals().items()):
    if not name.startswith("test_"):
      continue
    display_name = name.replace("_", " ").capitalize()
    success &= _run_test(display_name, func)

  print("")
  if not success:
    sys.exit(1)
