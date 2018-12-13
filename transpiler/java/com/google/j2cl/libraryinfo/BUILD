package(default_visibility = [
    "//tools:__subpackages__",
    "//transpiler:__subpackages__",
])

licenses(["notice"])  # Apache 2.0

proto_library(
    name = "library_info_proto",
    srcs = ["library_info.proto"],
)

java_proto_library(
    name = "library_info_java_proto",
    deps = [":library_info_proto"],
)

java_library(
    name = "libraryinfo",
    srcs = glob(["*.java"]),
    exports = [":library_info_java_proto"],
    deps = [
        ":library_info_java_proto",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/common",
    ],
)
