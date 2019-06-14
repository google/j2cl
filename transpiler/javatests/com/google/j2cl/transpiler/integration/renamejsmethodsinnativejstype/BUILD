load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

package(
    licenses = ["notice"],  # Apache 2.0
)

load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

j2cl_library(
    name = "foo",
    # Src's JUST the Foo class.
    srcs = ["Foo.java"],
    deps = [
        ":native_types",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
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
    native_srcs = ["Main.native.js"],
    deps = [
        ":foo",
        ":native_types",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

closure_js_library(
    name = "native_types",
    srcs = ["NativeFoo.js"],
)
