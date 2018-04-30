# Description:
#   Test for J2cl worker integration

# Apache2
licenses(["notice"])

java_library(
    name = "rerunning_transpiler",
    srcs = ["RerunningJ2clTranspiler.java"],
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/hash",
        "//java/com/google/common/io",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/frontend",
        "//transpiler/java/com/google/j2cl/transpiler:transpiler_lib",
    ],
)

java_binary(
    name = "RerunningJ2clTranspiler",
    jvm_flags = [
        "-Dj2cl.deterministicTimestamps=true",
    ],
    main_class = "com.google.j2cl.transpiler.RerunningJ2clTranspiler",
    runtime_deps = [":rerunning_transpiler"],
)

load("//build_def:j2cl_transpile.bzl", "j2cl_transpile")

# This target uses a special version of the transpiler that compiles code
# twice within the transpiler.
# This is intended to detect problems with static state in the transpiler.
j2cl_transpile(
    name = "jre_transpiled_twice",
    javalib = "//jre/java:jre_java_library",
    native_srcs_zips = ["//jre/java:jre_native.zip"],
    restricted_to = ["//buildenv/j2cl:j2cl_compilation"],
    # Turn off workers for this test since we are explicitly testing the compile and don't want the
    # transpile to run in a worker. See: b/62060418.
    supports_workers_internal = 0,
    transpiler = ":RerunningJ2clTranspiler",
)

load("//tools/build_rules:build_test.bzl", "build_test")

build_test(
    name = "jre_transpiling_twice_test",
    restricted_to = ["//buildenv/j2cl:j2cl_compilation"],
    targets = [
        ":jre_transpiled_twice",
    ],
)
