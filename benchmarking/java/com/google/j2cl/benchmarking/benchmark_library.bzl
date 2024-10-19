"""Helper to define benchmark support libraries"""

load("@rules_java//java:defs.bzl", "java_library")
load("//build_defs:rules.bzl", "j2cl_library", "j2wasm_library")
load("//build_defs/internal_do_not_use:j2cl_util.bzl", "absolute_label")

def benchmark_library(
        name,
        srcs,
        deps = [],
        tags = [],
        j2cl_srcs = None,
        j2cl_deps = None,
        j2wasm_srcs = None,
        j2wasm_deps = None):
    """Defines a benchmark_library that will compile in different java platforms."""

    deps = [absolute_label(dep) for dep in deps]
    j2cl_srcs = j2cl_srcs or srcs
    j2cl_deps = j2cl_deps or [dep + "-j2cl" for dep in deps]
    j2wasm_srcs = j2wasm_srcs or srcs
    j2wasm_deps = j2wasm_deps or [dep + "-j2wasm" for dep in deps]
    java_library(
        name = name,
        srcs = srcs,
        deps = deps,
        tags = tags,
    )

    j2wasm_library(
        name = name + "-j2wasm",
        srcs = j2wasm_srcs,
        deps = j2wasm_deps,
        tags = tags,
    )

    j2cl_library(
        name = name + "-j2cl",
        srcs = j2cl_srcs,
        deps = j2cl_deps,
        tags = tags,
    )
