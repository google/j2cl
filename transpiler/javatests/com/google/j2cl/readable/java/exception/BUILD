load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

readable_example(
    srcs = glob(["*.java"]),
    # TODO(b/201574151): Constructor Support
    build_kt_readables = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
