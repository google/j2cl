# Description:
#   Test for J2cl worker integration

# Apache2
licenses(["notice"])

java_binary(
    name = "RerunningJ2clTranspiler",
    srcs = ["RerunningJ2clTranspiler.java"],
    jvm_flags = [
        "-Dj2cl.deterministicTimestamps=true",
    ],
    deps = [
        "//third_party:auto_value",
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/bazel",
        "//transpiler/java/com/google/j2cl/transpiler:bazelbuilder_lib",
    ],
)

load("//build_defs:rules.bzl", "j2cl_library")

# This target uses a special version of the transpiler that compiles code
# twice within the transpiler.
# This is intended to detect problems with static state in the transpiler.
j2cl_library(
    name = "jre_transpiled_twice",
    srcs = ["//jre/java:jre_java_files"],
    _transpiler = ":RerunningJ2clTranspiler",
    native_srcs = ["//jre/java:jre_native_files"],
    deps = [
        "//third_party:gwt-javaemul-internal-annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

load("@bazel_tools//tools/build_rules:build_test.bzl", "build_test")

build_test(
    name = "jre_transpiling_twice_test",
    targets = [":jre_transpiled_twice"],
)
