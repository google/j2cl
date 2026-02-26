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
    # TODO(b/488029102): Enable once the bug is fixed.
    build_kt_readables = False,
    javacopts = [
        "-source 21",
        "-target 21",
    ],
)
