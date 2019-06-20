package(
    licenses = ["notice"],  # Apache 2.0
)

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/readable:readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
        "//third_party:jsr305_annotations-j2cl",
        "//third_party/java/checker_framework_annotations:checker_framework_annotations-j2cl",
    ],
)
