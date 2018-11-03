load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

# Apache2
licenses(["notice"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

closure_js_library(
    name = "Bar",
    srcs = ["foo.Bar.js"],
    deps_mgmt = "legacy",
    deps = ["//javascript/closure:base"],
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "staticjsmethods",
    srcs = glob(["*.java"]),
    js_deps = [":Bar"],
    native_srcs = ["Main.native.js"],
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
