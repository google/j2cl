load("@rules_java//java:defs.bzl", "java_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_library(
    name = "bazel",
    srcs = glob(["*.java"]),
    visibility = [
        "//tools:__subpackages__",
        "//transpiler:__subpackages__",
    ],
    deps = [
        ":worker_protocol_java_proto",
        "//third_party:args4j",
        "//third_party:error_prone_annotations",
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/common/bazel/profiler",
    ],
)

java_proto_library(
    name = "worker_protocol_java_proto",
    deps = [":worker_protocol_proto"],
)

proto_library(
    name = "worker_protocol_proto",
    srcs = ["worker_protocol.proto"],
)
