load("//build_defs:rules.bzl", "j2cl_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler/javatests/com/google/j2cl/readable:__subpackages__"],
    licenses = ["reciprocal"],
)

j2cl_library(
    name = "j2ktpublicdep",
    srcs = glob(["*.java"]),
    experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = True,
    deps = [
        "//third_party:jspecify_annotations-j2cl",
        "//transpiler/javatests/com/google/j2cl/readable/java/common",
    ],
)
