package(default_visibility = [
    "//tools:__subpackages__",
    "//transpiler:__subpackages__",
])

# Apache2
licenses(["notice"])

java_library(
    name = "common",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:apache_commons_lang3",
        "//third_party:auto_value",
        "//third_party:guava",
        "//third_party:jsr305_annotations",
    ],
)
