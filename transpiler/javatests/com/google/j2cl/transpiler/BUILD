# Description:
#   Test for J2cl worker integration

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_test(
    name = "J2clTranspilerWorkerTest",
    srcs = ["J2clTranspilerWorkerTest.java"],
    deps = [
        "//third_party/bazel/src/main/protobuf:worker_protocol_proto",
        "//third_party/java/junit",
        "//third_party/java/mockito",
        "//third_party/java/truth",
        "//transpiler/java/com/google/j2cl/problems",
        "//transpiler/java/com/google/j2cl/transpiler:transpiler_lib",
        "//transpiler/java/com/google/j2cl/transpiler:transpiler_worker_lib",
    ],
)

java_library(
    name = "transpiler_driver",
    srcs = ["J2clTranspilerDriver.java"],
    visibility = ["//:__subpackages__"],
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/hash",
        "//java/com/google/common/io",
        "//transpiler/java/com/google/j2cl/problems",
        "//transpiler/java/com/google/j2cl/transpiler:transpiler_lib",
    ],
)

java_library(
    name = "rerunning_transpiler",
    srcs = ["RerunningJ2clTranspiler.java"],
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/hash",
        "//java/com/google/common/io",
        "//transpiler/java/com/google/j2cl/frontend",
        "//transpiler/java/com/google/j2cl/problems",
        "//transpiler/java/com/google/j2cl/transpiler:transpiler_lib",
    ],
)

java_binary(
    name = "RerunningJ2clTranspiler",
    main_class = "com.google.j2cl.transpiler.RerunningJ2clTranspiler",
    runtime_deps = [":rerunning_transpiler"],
)

load("//build_def:j2cl_transpile.bzl", "j2cl_transpile")

# This target uses a special version of the transpiler that compiles code
# twice within the transpiler.
# This is intended to detect problems with static state in the transpiler.
j2cl_transpile(
    name = "jre_transpiled_twice",
    srcs = ["//jre/java:jre_hidden_java_files"],
    native_srcs_zips = ["//jre/java:jre_native_zips"],
    transpiler = ":RerunningJ2clTranspiler",
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations"],
)

load("//tools/build_rules:build_test.bzl", "build_test")

build_test(
    name = "jre_transpiling_twice_test",
    targets = [
        ":jre_transpiled_twice",
    ],
)
