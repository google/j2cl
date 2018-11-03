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
    name = "jsmethodoverride",
    srcs = glob(["*.java"]),
    native_srcs = [
        "Main.native.js",
    ],
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
