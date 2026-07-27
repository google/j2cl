load("@rules_java//java:defs.bzl", "java_test")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

java_test(
    name = "SourceMapComposerTest",
    srcs = ["SourceMapComposerTest.java"],
    deps = [
        "//third_party:error_prone_annotations",
        "//third_party:guava",
        "//third_party:jscomp",
        "//third_party:junit",
        "//third_party:truth",
        "//tools/java/com/google/j2cl/tools/sourcemapcomposer:sourcemapcomposer_lib",
        "//transpiler/java/com/google/j2cl/common",
    ],
)
