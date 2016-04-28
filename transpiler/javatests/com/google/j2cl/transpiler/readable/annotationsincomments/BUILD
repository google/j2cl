package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//transpiler:javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "annotationsincomments",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party/java/checker_framework:annotations-j2cl",
        "//third_party/java/j2cl:annotations-j2cl",
    ],
)
