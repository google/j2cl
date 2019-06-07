package(
    default_visibility = [
        "//tools:__subpackages__",
        "//transpiler:__subpackages__",
    ],
    licenses = ["notice"],  # Apache 2.0
)

java_library(
    name = "common",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:apache_commons_text",
        "//third_party:args4j",
        "//third_party:auto_value",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
    ],
)
