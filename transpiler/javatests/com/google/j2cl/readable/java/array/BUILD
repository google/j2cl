load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(licenses = ["notice"])

readable_example(
    srcs = glob(["*.java"]),
    # TODO(b/253061694): remove when the test is fixed.
    build_kt_readables = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
