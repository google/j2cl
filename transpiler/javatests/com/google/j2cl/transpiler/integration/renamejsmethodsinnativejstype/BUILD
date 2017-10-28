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
    name = "renamejsmethodsinnativejstype",
    # Src's JUST the Main class and gets Foo as a dependency. Do not
    # change this since this is part of what the test is verifying.
    srcs = ["Main.java"],
    js_deps = [":native_types"],
    native_srcs = ["Main.native.js"],
    deps = [
        ":Foo",
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
    ],
)

js_library(
    name = "native_types",
    srcs = ["NativeFoo.js"],
    deps_mgmt = "legacy",
    deps = ["//javascript/closure:base"],
)
