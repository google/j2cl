# Apache2
licenses(["notice"])

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
    defs = [
        # Make sure property renaming & variable renaming is turned off,
        # otherwise JsTypeTest fails since it asserts properties.
        # We need to turn both off here otherwise blaze fails with outputs not created.
        "--property_renaming=OFF",
        # We actually want variable renaming but have to turn it off due to bug: b/31816224
        "--variable_renaming=OFF",
    ],
    js_deps = [":native_types"],
    main_class = "woo.Main",
    native_srcs = glob(["*.native.js"]),
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations-j2cl"],
)

js_library(
    name = "native_types",
    srcs = glob(["native_types/*.js"]),
    deps_mgmt = "legacy",
    deps = ["//javascript/closure:base"],
)
