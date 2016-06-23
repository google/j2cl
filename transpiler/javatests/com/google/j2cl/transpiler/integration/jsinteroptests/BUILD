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
    name = "jsinteroptests",
    srcs = glob(["**/*.java"]),
    closure_defines = {"CAST_CHECKS_ENABLED_": "true"},
    js_deps = [":native_types"],
    main_class = "woo.Main",
    native_srcs = glob(["*.native.js"]),
    deps = ["//jre/java:gwt-jsinterop-annotations"],
)

js_library(
    name = "native_types",
    srcs = glob(["native_types/*.js"]),
)
