# Apache2
licenses(["notice"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

java_library(
    name = "gwtincompatibleMain",
    srcs = ["Main.java"],
)

java_library(
    name = "gwtincompatibleOther",
    srcs = ["Other.java"],
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "gwtincompatible-with-srcjar",
    srcs = [
        "External.java",
        ":libgwtincompatibleMain-src.jar",
        ":libgwtincompatibleOther-src.jar",
    ],
    deps = [
        "//java/com/google/common/annotations:annotations-j2cl",
        "//java/com/google/common/io:io-j2cl",
    ],
)
