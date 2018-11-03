# Apache2
licenses(["notice"])

load("//build_defs:rules.bzl", "j2cl_library")
load("//tools/javatests/com/google/j2cl/tools/rta:rta_test.bzl", "rta_test")

j2cl_library(
    name = "unusedmethods_library",
    srcs = ["SimpleClass.java"],
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)

rta_test(
    name = "unusedmethods",
    targets = [":unusedmethods_library"],
    unused_members_golden_file = "unused_members.txt",
    unused_types_golden_file = "unused_types.txt",
)
