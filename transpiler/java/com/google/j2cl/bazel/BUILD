package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

java_library(
    name = "bazel",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party/bazel/src/main/protobuf:worker_protocol_proto",
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/frontend",
    ],
)
