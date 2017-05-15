package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)
load("//build_def:j2cl_util.bzl", "generate_zip")

generate_zip(
    name = "gwtincompatibleMain.srcjar",
    srcs = ["Main.java"],
    pkg = "RELATIVE",
)

generate_zip(
    name = "gwtincompatibleOther.srcjar",
    srcs = ["Other.java"],
    pkg = "RELATIVE",
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "gwtincompatible-with-srcjar",
    srcs = [
        "External.java",
        ":gwtincompatibleMain.srcjar",
        ":gwtincompatibleOther.srcjar",
    ],
    deps = [
        "//java/com/google/common/annotations:annotations-j2cl",
        "//java/com/google/common/io:io-j2cl",
    ],
)
