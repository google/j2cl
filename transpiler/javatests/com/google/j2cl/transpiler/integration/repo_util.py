#!/usr/bin/python2.7
#
# Copyright 2015 Google Inc. All Rights Reserved.

"""Util funcs for running Blaze on int. tests in managed and unmanaged repo."""


import os
from os.path import expanduser


import process_util


# pylint: disable=global-variable-not-assigned
TEST_TARGET_PATTERN = ("third_party/java_src/j2cl/transpiler/javatests/"
                       "com/google/j2cl/transpiler/integration/...:all")
OBFUSCATED_OPT_TEST_PATTERN = (
    "//third_party/java_src/j2cl/transpiler/javatests/"
    "com/google/j2cl/transpiler/integration/"
    "%s:optimized_js")
OBFUSCATED_OPT_TEST_FILE = (
    "blaze-bin/third_party/java_src/j2cl/transpiler/javatests/"
    "com/google/j2cl/transpiler/integration/"
    "%s/optimized_js.js")
READABLE_OPT_TEST_PATTERN = ("//third_party/java_src/j2cl/transpiler/javatests/"
                             "com/google/j2cl/transpiler/integration/"
                             "%s:readable_optimized_js")
READABLE_OPT_TEST_FILE = (
    "blaze-bin/third_party/java_src/j2cl/transpiler/javatests/"
    "com/google/j2cl/transpiler/integration/"
    "%s/readable_optimized_js.js")
READABLE_TEST_PATTERN = ("//third_party/java_src/j2cl/transpiler/javatests/"
                         "com/google/j2cl/transpiler/integration/"
                         "%s:readable_unoptimized_js")
READABLE_TEST_FILE = (
    "blaze-bin/third_party/java_src/j2cl/transpiler/javatests/"
    "com/google/j2cl/transpiler/integration/"
    "%s/readable_unoptimized_js.js")
HOME_DIR_PATH = expanduser("~")
MANAGED_REPO_PATH = HOME_DIR_PATH + "/.j2cl-size-repo"
MANAGED_GOOGLE3_PATH = MANAGED_REPO_PATH + "/google3"
JAVA8_BOOT_CLASS_PATH = ("--javac_bootclasspath="
                         "//third_party/java/jdk:langtools8-bootclasspath")


def build_optimized_tests(cwd=None):
  """Blaze builds all integration tests in parallel."""
  process_util.run_cmd_get_output(
      ["blaze", "build", TEST_TARGET_PATTERN, JAVA8_BOOT_CLASS_PATH], cwd=cwd)


def get_obfuscated_optimized_test_file(test_name):
  """Returns the path to the obfuscated opt JS file the given test."""
  global OBFUSCATED_OPT_TEST_FILE

  return OBFUSCATED_OPT_TEST_FILE % test_name


def build_obfuscated_optimized_test(test_name, cwd=None):
  """Blaze builds the obfuscated opt JS for a particular test."""
  global OBFUSCATED_OPT_TEST_PATTERN

  process_util.run_cmd_get_output(
      ["blaze", "build", OBFUSCATED_OPT_TEST_PATTERN % test_name], cwd=cwd)


def get_readable_optimized_test_file(test_name):
  """Returns the path to the readable opt JS file the given test."""
  global READABLE_OPT_TEST_FILE

  return READABLE_OPT_TEST_FILE % test_name


def build_readable_optimized_test(test_name, cwd=None):
  """Blaze builds the readable opt JS for a particular test."""
  global READABLE_OPT_TEST_PATTERN

  process_util.run_cmd_get_output(
      ["blaze", "build", READABLE_OPT_TEST_PATTERN % test_name], cwd=cwd)


def get_readable_unoptimized_test_file(test_name):
  """Returns the path to the readable unoptimized JS file the given test."""
  global READABLE_TEST_FILE

  return READABLE_TEST_FILE % test_name


def build_readable_unoptimized_test(test_name, cwd=None):
  """Blaze builds the readable unoptimized JS for a particular test."""
  global READABLE_TEST_PATTERN

  process_util.run_cmd_get_output(
      ["blaze", "build", READABLE_TEST_PATTERN % test_name], cwd=cwd)


def compute_synced_to_cl():
  """Returns the cl that git5 is currently synced to."""
  status_line = process_util.run_cmd_get_output(["git5", "status"])
  synced_to_cl = process_util.extract_pattern(
      "Synced at CL (.*?) = ", status_line)
  return int(synced_to_cl)


def get_js_files_by_test_name(cwd=None):
  """Finds and returns a test_name<->optimized_js_file map."""
  # Gather a list of the names of the test targets we care about
  test_targets = (
      process_util.run_cmd_get_output(
          ["blaze", "query",
           "filter('.*:optimized_js', kind(%s, %s))" %
           ("js_binary", TEST_TARGET_PATTERN)], cwd=cwd).split("\n"))
  test_targets = filter(bool, test_targets)

  # Convert to a map of names<->jsFile pairs
  test_names = [
      process_util.extract_pattern(
          ".*integration/(.*?):optimized_js", size_target)
      for size_target in test_targets]
  js_files = [
      size_target.replace("//", "blaze-bin/").replace(":", "/") + ".js"
      for size_target in test_targets]
  return dict(zip(test_names, js_files))


def managed_repo_sync_to(cl):
  process_util.run_cmd_get_output(
      ["git5", "sync", "@" + str(cl), "--rebase"], cwd=MANAGED_GOOGLE3_PATH)


def managed_repo_validate_environment():
  """Ensure expected directories exist."""
  global MANAGED_REPO_PATH

  if not os.path.isdir(MANAGED_REPO_PATH):
    print("  Creating managed opt size tracking git5 repo at '%s'" %
          MANAGED_REPO_PATH)
    os.mkdir(MANAGED_REPO_PATH)
    process_util.run_cmd_get_output(
        ["git5", "start", "base", "//depot/google3/third_party/java_src/j2cl"],
        cwd=MANAGED_REPO_PATH)
