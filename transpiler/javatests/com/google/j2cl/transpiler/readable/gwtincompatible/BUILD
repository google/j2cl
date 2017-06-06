package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "gwtincompatible",
    srcs = [
        "Implementor.java",
    ],
    deps = [
        ":interface",
        "//java/com/google/common/annotations:annotations-j2cl",
    ],
)
