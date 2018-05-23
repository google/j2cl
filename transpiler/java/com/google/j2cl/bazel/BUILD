# Apache2
licenses(["notice"])

java_library(
    name = "bazel",
    srcs = glob(["*.java"]),
    visibility = [
        "//tools:__subpackages__",
        "//transpiler:__subpackages__",
    ],
    deps = [
        ":worker_protocol_java_proto",
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/frontend",
    ],
)

java_proto_library(
    name = "worker_protocol_java_proto",
    has_services = 0,
    deps = [":worker_protocol_proto"],
)

proto_library(
    name = "worker_protocol_proto",
    srcs = ["worker_protocol.proto"],
    has_services = 0,
    java_api_version = 2,
)
