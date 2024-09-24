# Description:
#  Tests for a minifier library for J2CL mangled names.

load("@rules_java//java:defs.bzl", "java_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_test(
    name = "J2clMinifierTest",
    srcs = ["J2clMinifierTest.java"],
    deps = [
        "//third_party:junit",
        "//tools/java/com/google/j2cl/tools/minifier",
    ],
)

java_test(
    name = "CodePruningTest",
    srcs = ["CodePruningTest.java"],
    deps = [
        "//third_party:guava",
        "//third_party:junit",
        "//third_party:truth",
        "//tools/java/com/google/j2cl/tools/minifier",
        "//tools/java/com/google/j2cl/tools/rta:code_removal_info_java_proto",
    ],
)
