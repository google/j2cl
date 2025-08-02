load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = glob(["*.kt"]),
    generate_kt_readables = False,
    deps = ["//transpiler/javatests/com/google/j2cl/readable/java/xplatlogger:fakes"],
)
