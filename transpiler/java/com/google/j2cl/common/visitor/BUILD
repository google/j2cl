load("//build_defs:rules.bzl", "j2cl_import")

package(
    default_visibility = [
        "@com_google_jsinterop_generator//:__subpackages__",
        "//transpiler:__subpackages__",
    ],
    licenses = ["notice"],  # Apache 2.0
)

java_library(
    name = "visitor",
    exported_plugins = ["//transpiler/java/com/google/j2cl/common/visitor/generator:visitor-apt-processor"],
    exports = [":visitor-internal"],
)

java_library(
    name = "visitor-internal",
    srcs = glob(["*.java"]),
)

j2cl_import(
    name = "visitor-j2cl",
    jar = ":visitor",
)
