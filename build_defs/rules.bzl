"""Bazel definitions for J2CL.

See corresponding bzl files for the documentation.
"""

load("//build_defs/internal_do_not_use:repository.bzl", _setup_j2cl_workspace = "setup_j2cl_workspace")
load("//build_defs/internal_do_not_use:j2cl_application.bzl", _j2cl_application = "j2cl_application")
load("//build_defs/internal_do_not_use:j2cl_library.bzl", _j2cl_library = "j2cl_library")
load("//build_defs/internal_do_not_use:j2cl_import.bzl", _j2cl_import = "j2cl_import")
load(
    "//build_defs/internal_do_not_use:j2cl_js_common.bzl",
    _J2CL_OPTIMIZED_DEFS = "J2CL_OPTIMIZED_DEFS",
)

setup_j2cl_workspace = _setup_j2cl_workspace

j2cl_application = _j2cl_application

j2cl_library = _j2cl_library

j2cl_import = _j2cl_import


J2CL_OPTIMIZED_DEFS = _J2CL_OPTIMIZED_DEFS
