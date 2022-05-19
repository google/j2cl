package(
    default_visibility = ["//transpiler:__subpackages__"],
    licenses = ["notice"],
)

java_library(
    name = "wasm",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:auto_value",
        "//third_party:gson",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/backend/common",
    ],
)
