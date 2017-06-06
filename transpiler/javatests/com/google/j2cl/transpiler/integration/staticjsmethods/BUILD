package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

js_library(
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
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations-j2cl"],
)
