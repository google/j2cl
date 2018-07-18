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
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/hash",
        "//java/com/google/common/io",
        "//third_party/java/auto:auto_value",
        "//transpiler/java/com/google/j2cl/bazel",
        "//transpiler/java/com/google/j2cl/transpiler:bazelbuilder_lib",
    ],
)

load("//tools/build_rules:build_test.bzl", "build_test")

build_test(
    name = "jre_transpiling_twice_test",
    targets = [":jre_transpiled_twice"],
)
