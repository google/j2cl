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
    _declare_legacy_namespace = True,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
