package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "defaultnonnullable",
    srcs = glob(["*.java"]),
    deps = [
        ":nonnullablesubpackage",
        ":nullablesubpackage",
        "//java/com/google/common/base:base-j2cl",
        "//java/com/google/common/collect:collect-j2cl",
        "//third_party/java/checker_framework:annotations-j2cl",
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
        "//third_party/java/j2cl:annotations-j2cl",
        "//third_party/java/jsr305_annotations:jsr305_annotations-j2cl",
    ],
)
