load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = glob(["*.java"]),
    # TODO(b/202427872): Support inner classes and interfaces in J2KT.
    # TODO(b/201574151): Constructor Support.
    build_kt_readables = False,
)
