licenses(["notice"])  # Apache 2.0

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
    srcs = glob([
        "**/*.java",
        "native_types/*.js",
    ]),
    defs = [
        "--strict",
        "--emit_use_strict=true",
        # Make sure property renaming & variable renaming is turned off,
        # otherwise JsTypeTest fails since it asserts properties.
        # We need to turn both off here otherwise blaze fails with outputs not created.
        "--property_renaming=OFF",
        # We actually want variable renaming but have to turn it off due to bug: b/31816224
        "--variable_renaming=OFF",
    ],
    main_class = "woo.Main",
    native_srcs = glob(["*.native.js"]),
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
