# Apache2
licenses(["notice"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
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
