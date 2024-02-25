"""Bazel definitions for J2CL.

See corresponding bzl files for the documentation.
"""

load("//build_defs/internal_do_not_use:gen_j2cl_tests.bzl", _gen_j2cl_tests = "gen_j2cl_tests")
load("//build_defs/internal_do_not_use:gen_j2kt_tests.bzl", _gen_j2kt_native_tests = "gen_j2kt_native_tests")
load("//build_defs/internal_do_not_use:gen_j2wasm_tests.bzl", _gen_j2wasm_tests = "gen_j2wasm_tests")
load("//build_defs/internal_do_not_use:j2cl_alias.bzl", _j2cl_alias = "j2cl_alias")
load("//build_defs/internal_do_not_use:j2cl_application.bzl", _j2cl_application = "j2cl_application")
load("//build_defs/internal_do_not_use:j2cl_import.bzl", _j2cl_import = "j2cl_import")
load(
    "//build_defs/internal_do_not_use:j2cl_js_common.bzl",
    _J2CL_OPTIMIZED_DEFS = "J2CL_OPTIMIZED_DEFS",
    _J2CL_TEST_DEFS = "J2CL_TEST_DEFS",
)
load("//build_defs/internal_do_not_use:j2cl_library.bzl", _j2cl_library = "j2cl_library")
load("//build_defs/internal_do_not_use:j2cl_repo.bzl", _j2cl_import_external = "j2cl_import_external", _j2cl_maven_import_external = "j2cl_maven_import_external")
load("//build_defs/internal_do_not_use:j2cl_rta.bzl", _j2cl_rta = "j2cl_rta")
load("//build_defs/internal_do_not_use:j2cl_test.bzl", _j2cl_test = "j2cl_test")
load("//build_defs/internal_do_not_use:j2kt_import.bzl", _j2kt_jvm_import = "j2kt_jvm_import", _j2kt_native_import = "j2kt_native_import")
load(
    "//build_defs/internal_do_not_use:j2kt_library.bzl",
    _j2kt_apple_framework = "j2kt_apple_framework",
    _j2kt_jvm_library = "j2kt_jvm_library",
    _j2kt_minimal_apple_framework = "j2kt_minimal_apple_framework",
    _j2kt_native_library = "j2kt_native_library",
    _j2kt_precompilied_apple_framework = "j2kt_precompilied_apple_framework",
)
load("//build_defs/internal_do_not_use:j2kt_test.bzl", _j2kt_jvm_test = "j2kt_jvm_test", _j2kt_native_test = "j2kt_native_test")
load("//build_defs/internal_do_not_use:j2wasm_application.bzl", _j2wasm_application = "j2wasm_application")
load("//build_defs/internal_do_not_use:j2wasm_library.bzl", _j2wasm_library = "j2wasm_library")
load("//build_defs/internal_do_not_use:j2wasm_test.bzl", _j2wasm_test = "j2wasm_test")

j2cl_application = _j2cl_application

j2cl_library = _j2cl_library

j2cl_import = _j2cl_import

j2cl_alias = _j2cl_alias

j2cl_import_external = _j2cl_import_external

j2cl_maven_import_external = _j2cl_maven_import_external

j2cl_rta = _j2cl_rta

j2cl_test = _j2cl_test

gen_j2cl_tests = _gen_j2cl_tests

gen_j2wasm_tests = _gen_j2wasm_tests

gen_j2kt_native_tests = _gen_j2kt_native_tests

j2kt_apple_framework = _j2kt_apple_framework

j2kt_precompiled_apple_framework = _j2kt_precompilied_apple_framework

j2kt_minimal_apple_framework = _j2kt_minimal_apple_framework

j2kt_native_library = _j2kt_native_library

j2kt_jvm_library = _j2kt_jvm_library

j2kt_native_import = _j2kt_native_import

j2kt_jvm_import = _j2kt_jvm_import

j2kt_jvm_test = _j2kt_jvm_test

j2kt_native_test = _j2kt_native_test

j2wasm_application = _j2wasm_application

j2wasm_library = _j2wasm_library

j2wasm_test = _j2wasm_test

J2CL_OPTIMIZED_DEFS = _J2CL_OPTIMIZED_DEFS

J2CL_TEST_DEFS = _J2CL_TEST_DEFS
