package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = [
        "//tools:__subpackages__",
        "//transpiler:__subpackages__",
    ],
    licenses = ["notice"],
)

proto_library(
    name = "summary_proto",
    srcs = ["summary.proto"],
)

java_proto_library(
    name = "summary_java_proto",
    deps = [":summary_proto"],
)

java_library(
    name = "wasm",
    srcs = glob(["*.java"]),
    deps = [
        ":summary_java_proto",
        "//third_party:auto_value",
        "//third_party:gson",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
        "//third_party:protobuf_util_json",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/backend/closure",
        "//transpiler/java/com/google/j2cl/transpiler/backend/common",
    ],
)
