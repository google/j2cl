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
    # TODO(b/202430789): Support boxing/unboxing of primitive conversions in J2KT
    # TODO(b/202513994): Support various arithmetic operations.
    build_kt_readables = False,
)
