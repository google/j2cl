package(licenses = ["notice"])

java_library(
    name = "visitor",
    exported_plugins = ["//transpiler/java/com/google/j2cl/common/visitor/generator:visitor-apt-processor"],
    visibility = [
        "@com_google_jsinterop_generator//:__subpackages__",
        "//transpiler:__subpackages__",
    ],
    exports = [":visitor-internal"],
)

java_library(
    name = "visitor-internal",
    srcs = glob(["*.java"]),
    visibility = ["//transpiler/java/com/google/j2cl/common/visitor/generator:__pkg__"],
)
