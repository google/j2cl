"""Bazel definitions for J2CL.

See corresponding bzl files for the documentation.
"""

load("//build_defs/internal_do_not_use:j2cl_library.bzl", _j2cl_library = "j2cl_library")
load("//build_defs/internal_do_not_use:j2cl_import.bzl", _j2cl_import = "j2cl_import")
load("//build_defs/internal_do_not_use:j2cl_rta.bzl", _j2cl_rta = "j2cl_rta")
load("//build_defs/internal_do_not_use:j2cl_test.bzl", _j2cl_test = "j2cl_test")
load("//build_defs/internal_do_not_use:gen_j2cl_tests.bzl", _gen_j2cl_tests = "gen_j2cl_tests")
load(
    "//build_defs/internal_do_not_use:j2cl_js_common.bzl",
    _J2CL_OPTIMIZED_DEFS = "J2CL_OPTIMIZED_DEFS",
    _J2CL_TEST_DEFS = "J2CL_TEST_DEFS",
)

j2cl_library = _j2cl_library

j2cl_import = _j2cl_import

j2cl_rta = _j2cl_rta

j2cl_test = _j2cl_test

gen_j2cl_tests = _gen_j2cl_tests

J2CL_OPTIMIZED_DEFS = _J2CL_OPTIMIZED_DEFS

J2CL_TEST_DEFS = _J2CL_TEST_DEFS
