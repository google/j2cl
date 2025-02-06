load("@rules_java//java:defs.bzl", "java_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

java_library(
    name = "visitor",
    exported_plugins = ["//transpiler/java/com/google/j2cl/common/visitor/generator:visitor-apt-processor"],
    visibility = [
        # Do not use. Temporary visible to workaround https://github.com/bazelbuild/bazel/issues/25214.
        "//visibility:public",
    ],
    exports = [":visitor-internal"],
)

java_library(
    name = "visitor-internal",
    srcs = glob(["*.java"]),
    visibility = ["//transpiler/java/com/google/j2cl/common/visitor/generator:__pkg__"],
)
