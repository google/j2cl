"""Checks JavaScript proxy declarations and real library outputs."""

load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("//build_defs:rules.bzl", "j2cl_library")

def _proxy_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(env, ctx.attr.expected, ctx.attr.actual)
    return unittest.end(env)

_proxy_test = unittest.make(
    _proxy_test_impl,
    attrs = {"actual": attr.bool(), "expected": attr.bool()},
)

def library_proxy_test_suite(name):
    j2cl_library(
        name = "with_sources",
        srcs = ["Example.java"],
        generate_build_test = False,
        generate_j2wasm_library = False,
    )
    j2cl_library(name = "empty", generate_j2wasm_library = False)
    j2cl_library(
        name = "exports_only",
        exports = [":with_sources"],
        generate_j2wasm_library = False,
    )
    tests = []
    for subject, expected in [
        ("empty", False),
        ("exports_only", False),
        ("with_sources", True),
    ]:
        test = subject + "_proxy_test"
        _proxy_test(
            name = test,
            actual = native.existing_rule(subject + ".js") != None,
            expected = expected,
        )
        tests.append(test)
    build_test(
        name = "library_outputs_test",
        targets = [":empty", ":exports_only", ":with_sources.js"],
    )
    native.test_suite(name = name, tests = tests + [":library_outputs_test"])
