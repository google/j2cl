package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = [
        "//tools:__subpackages__",
        "//transpiler:__subpackages__",
    ],
    licenses = ["notice"],
)

java_library(
    name = "common",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:args4j",
        "//third_party:auto_value",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
    ],
)
