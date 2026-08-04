# Description:
#  Implementation of source map composer for J2CL.

load("@rules_java//java:defs.bzl", "java_library")
load("//transpiler/java/com/google/j2cl/common/bazel:compiler_java_binary.bzl", "compiler_java_binary")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

java_library(
    name = "sourcemapcomposer_lib",
    srcs = glob(
        ["*.java"],
        exclude = ["BazelSourceMapComposer.java"],
    ),
    visibility = ["//tools/javatests/com/google/j2cl/tools/sourcemapcomposer:__pkg__"],
    deps = [
        "//third_party:jscomp",
        "//third_party:jspecify_annotations",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/common",
    ],
)

java_library(
    name = "sourcemapcomposer_worker_lib",
    srcs = ["BazelSourceMapComposer.java"],
    deps = [
        ":sourcemapcomposer_lib",
        "//third_party:args4j",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/common/bazel",
    ],
)

compiler_java_binary(
    name = "SourceMapComposer_worker",
    main_class = "com.google.j2cl.tools.sourcemapcomposer.BazelSourceMapComposer",
    runtime_deps = [":sourcemapcomposer_worker_lib"],
)
