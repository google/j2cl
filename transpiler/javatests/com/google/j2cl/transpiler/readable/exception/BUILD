load(
    "//transpiler/javatests/com/google/j2cl/transpiler/readable:readable_example.bzl",
    "readable_example",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

readable_example(
    srcs = glob(["*.java"]),
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
