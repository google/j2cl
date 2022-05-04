load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(licenses = ["notice"])

readable_example(
    srcs = glob(["*.java"]),
    # TODO(b/204594816): Render explicit labels in return from lambdas.
    build_kt_readables = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
