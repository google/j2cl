package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "nativejstypes",
    srcs = glob(["*.java"]),
    js_deps = [":native_types"],
    deps = ["//jre/java:gwt-jsinterop-annotations"],
)

js_library(
    name = "native_types",
    srcs = glob(["*.js"]),
    deps_mgmt = "legacy",
    deps = ["//javascript/closure:base"],
)
