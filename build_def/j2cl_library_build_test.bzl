"""Build test macro for j2cl_library targets. Not intended for public use."""

load("//build_def:j2cl_js_common.bzl", "J2CL_OPTIMIZED_DEFS")
load("//tools/build_rules:build_test.bzl", build_test_rule = "build_test")

def build_test(target, tags):
    """Create a <target>_build_test that verifies the provied J2CL target"""

    # exit early to avoid parse errors when running under bazel
    if not hasattr(native, "js_library"):
        return

    # Add an empty .js file to the js_binary build test compilation so that  jscompiler does not
    # error out when there are no .js source (e.g. all sources are @JsFunction).
    native.genrule(
        name = target + "_empty_js_file",
        cmd = "echo \"// empty file\" > $(OUTS)",
        outs = [target + "_empty_js_file.js"],
    )
    native.js_library(
        name = target + "_empty_js_file_lib",
        srcs = [target + "_empty_js_file"],
        tags = ["no_grok"],
    )
    native.js_binary(
        name = target + "_js_binary",
        deps = [
            target,
            target + "_empty_js_file_lib",
        ],
        defs = J2CL_OPTIMIZED_DEFS,
        tags = tags + ["avoid_dep", "no_grok"],
        compiler = "//javascript/tools/jscompiler:head",
        testonly = 1,
        visibility = ["//visibility:private"],
    )

    build_test_rule(
        name = target + "_build_test",
        targets = [
            target,
            target + "_js_binary",
        ],
        tags = tags + ["avoid_dep", "no_grok"],
    )
