"""Verify extra_defs reach the Closure compiler created by j2cl_test."""

load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//build_defs:rules.bzl", "j2cl_test")

def _definitions_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(env, ["--define=goog.DEBUG=false"], ctx.attr.compiler_defs)
    return unittest.end(env)

_definitions_test = unittest.make(
    _definitions_test_impl,
    attrs = {"compiler_defs": attr.string_list()},
)

def test_definitions_suite(name):
    j2cl_test(
        name = "subject",
        runtime_deps = ["//:junit"],
        test_class = "example.ExampleTest",
        extra_defs = ["--define=goog.DEBUG=false"],
        tags = ["manual"],
    )
    _definitions_test(
        name = name,
        compiler_defs = native.existing_rule("subject_bin")["defs"],
    )
