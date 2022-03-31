load("//build_defs:rules.bzl", "j2cl_library")
load("//tools/javatests/com/google/j2cl/tools/rta:rta_test.bzl", "rta_test")

package(
    licenses = ["notice"],  # Apache 2.0
)

j2cl_library(
    name = "emptyconstructor_library",
    srcs = [
        "Child.java",
        "Parent.java",
    ],
    javacopts = [
        "-XepOpt:CheckReturnValue:CheckAllConstructors=false",  # b/226969262
    ],
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)

rta_test(
    name = "emptyconstructor",
    targets = [":emptyconstructor_library"],
    unused_types_golden_file = "unused_types.txt",
)
