licenses(["notice"])  # Apache 2.0

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    srcs = glob(["*.java"]),
    # TODO(b/118184493): remove when Java 9 language support is enabled.
    j2cl_library_tags = ["manual"],
)
