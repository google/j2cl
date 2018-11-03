# Apache2
licenses(["notice"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    srcs = glob(["*.java"]),
    deps = [
        ":subpackage",
        "//java/com/google/common/base:base-j2cl",
        "//java/com/google/common/collect:collect-j2cl",
        "//third_party/java/checker_framework:annotations-j2cl",
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
        "//third_party/java/jsr305_annotations:jsr305_annotations-j2cl",
    ],
)

load("//build_defs:rules.bzl", "j2cl_library")

j2cl_library(
    name = "subpackage",
    srcs = glob(["subpackage/*.java"]),
    deps = [
        "//third_party/java/jsr305_annotations:jsr305_annotations-j2cl",
    ],
)
