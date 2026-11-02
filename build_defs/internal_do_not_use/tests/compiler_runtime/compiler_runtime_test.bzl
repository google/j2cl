"""Check J2CL and J2Wasm use the selected tool runtime."""

load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("@rules_java//java:defs.bzl", "java_common")

def _runtime_test_impl(ctx):
    env = unittest.begin(ctx)
    expected = ctx.attr._runtime[java_common.JavaRuntimeInfo].java_home
    for toolchain in [ctx.attr._j2cl, ctx.attr._j2wasm]:
        asserts.equals(env, expected, toolchain[java_common.JavaToolchainInfo].java_runtime.java_home)
    return unittest.end(env)

compiler_runtime_test = unittest.make(
    _runtime_test_impl,
    attrs = {
        "_j2cl": attr.label(default = "//jre/java:j2cl_java_toolchain"),
        "_j2wasm": attr.label(default = "//jre/java:j2wasm_java_toolchain"),
        "_runtime": attr.label(default = "@rules_java//toolchains:current_host_java_runtime"),
    },
)
