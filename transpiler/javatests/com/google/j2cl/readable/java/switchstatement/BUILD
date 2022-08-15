load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(licenses = ["notice"])

readable_example(
    srcs = glob(["*.java"]),
    # TODO(b/242217340): Convert case expressions to have the same type as
    # switch expression, which is required in Kotlin's `when`.
    build_kt_readables = False,
)
