load("//build_defs:rules.bzl", "j2cl_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler/javatests/com/google/j2cl/readable:__subpackages__"],
    licenses = ["notice"],
)

j2cl_library(
    name = "foowithpackageinfo-j2cl",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:j2objc_annotations-j2cl",
    ],
)
