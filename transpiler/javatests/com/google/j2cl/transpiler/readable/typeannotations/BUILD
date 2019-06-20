package(
    licenses = ["notice"],  # Apache 2.0
)

load("//third_party/java/j2cl:j2cl_library.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/transpiler/readable:readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    srcs = ["TypeAnnotations.java"],
    deps = [
        ":parametric-type",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

j2cl_library(
    name = "parametric-type",
    # ParametricType is explicitly defined in a separate library to test propagation of
    # type annotations across compilation boundaries.
    srcs = ["ParametricType.java"],
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
