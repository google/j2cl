# Description:
#  Tests for a minifier library for J2CL mangled names.

load("//build_defs:rules.bzl", "j2cl_test")
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

j2cl_test(
    name = "J2clMinifierTest-j2cl",
    srcs = ["J2clMinifierTest.java"],
    # Unforunately, we cannot run J2clMinifer on the test of J2clMinifier itself since J2clMinifier
    # by design also minifies quoted identifiers. That messes up with the test cases which have
    # inherently have such identifiers since it is also testing re-writing of such patterns.
    # For example; assertChange("m_foo__", " foo_$1 ") replaced with
    # assertChange("foo_$NNN", "foo_$1 ") which is no longer a valid test case.
    jvm_flags = ["-Dj2cl_minifier_disabled=true"],
    deps = [
        "//third_party:junit-j2cl",
        "//tools/java/com/google/j2cl/tools/minifier:minifier-j2cl",
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

j2cl_test(
    name = "CodePruningTest-j2cl",
    srcs = ["CodePruningTest.java"],
    deps = [
        "//third_party:guava-j2cl",
        "//third_party:junit-j2cl",
        "//third_party:truth-j2cl",
        "//tools/java/com/google/j2cl/tools/minifier:minifier-j2cl",
        "//tools/java/com/google/j2cl/tools/rta:code_removal_info_j2cl_proto",
    ],
)
