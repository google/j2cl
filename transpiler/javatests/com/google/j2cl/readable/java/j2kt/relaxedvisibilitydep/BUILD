load("//build_defs:rules.bzl", "j2cl_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler/javatests/com/google/j2cl/readable:__subpackages__"],
    licenses = ["reciprocal"],
)

j2cl_library(
    name = "relaxedvisibilitydep",
    srcs = glob(["*.java"]),
)
