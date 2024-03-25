load("//build_defs:rules.bzl", "j2cl_library")
load("//tools/javatests/com/google/j2cl/tools/rta:rta_test.bzl", "rta_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "nativemethod_library",
    srcs = glob(["*.java"]),
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

rta_test(
    name = "nativemethod",
    targets = [":nativemethod_library"],
    unused_types_golden_file = "unused_types.txt",
)
