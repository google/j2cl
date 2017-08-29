package(default_visibility = ["//:__subpackages__"])

# Apache2
licenses(["notice"])

java_test(
    name = "BaseWorkerTest",
    srcs = ["BaseWorkerTest.java"],
    deps = [
        "//third_party/bazel/src/main/protobuf:worker_protocol_proto",
        "//third_party/java/junit",
        "//third_party/java/mockito",
        "//third_party/java/truth",
        "//transpiler/java/com/google/j2cl/bazel",
    ],
)
