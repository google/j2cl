# Apache2
licenses(["notice"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)
load("//build_defs:rules.bzl", "j2cl_library")

j2cl_library(
    name = "interface",
    srcs = [
        "Interface.java",
    ],
    deps = [
        "//java/com/google/common/annotations:annotations-j2cl",
    ],
)

# Creates verification target
readable_example(
    srcs = [
        "Implementor.java",
    ],
    deps = [
        ":interface",
        "//java/com/google/common/annotations:annotations-j2cl",
    ],
)
