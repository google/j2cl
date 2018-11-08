licenses(["notice"])  # Apache 2.0

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
