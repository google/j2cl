load("//build_defs:rules.bzl", "j2cl_library")
load("//tools/javatests/com/google/j2cl/tools/rta:rta_test.bzl", "rta_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "jsenum_library",
    srcs = [
        "Main.java",
    ],
    javacopts = [
        "-XepOpt:CheckReturnValue:CheckAllConstructors=false",  # b/226969262
    ],
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)

rta_test(
    name = "jsenum",
    targets = [":jsenum_library"],
    unused_types_golden_file = "unused_types.txt",
)
