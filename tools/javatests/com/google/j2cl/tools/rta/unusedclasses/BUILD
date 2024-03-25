load("//build_defs:rules.bzl", "j2cl_library")
load("//tools/javatests/com/google/j2cl/tools/rta:rta_test.bzl", "rta_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "unusedclasses_library",
    srcs = [
        "SimpleClass.java",
        "UnusedClass.java",
    ],
    javacopts = [
    ],
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

rta_test(
    name = "unusedclasses",
    targets = [":unusedclasses_library"],
    unused_types_golden_file = "unused_types.txt",
)
