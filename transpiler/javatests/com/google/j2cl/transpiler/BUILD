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
        "//transpiler/java/com/google/j2cl/errors",
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
        "//transpiler/java/com/google/j2cl/errors",
        "//transpiler/java/com/google/j2cl/transpiler:transpiler_lib",
    ],
)

java_library(
    name = "rerunning_transpiler",
    srcs = ["RerunningJ2clTranspiler.java"],
    visibility = [
        "//third_party/java/j2cl:__subpackages__",
        "//:__subpackages__",
    ],
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/hash",
        "//java/com/google/common/io",
        "//transpiler/java/com/google/j2cl/errors",
        "//transpiler/java/com/google/j2cl/frontend",
        "//transpiler/java/com/google/j2cl/transpiler:transpiler_lib",
    ],
)
