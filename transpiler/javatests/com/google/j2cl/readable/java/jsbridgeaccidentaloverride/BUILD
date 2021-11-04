load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

# Creates verification target
readable_example(
    srcs = glob(["*.java"]),
    # TODO(b/204587514): J2KT needs better nullability support
    build_kt_readables = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
