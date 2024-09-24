load("//build_defs:rules.bzl", "j2cl_import", "j2cl_library")
load("@rules_java//java:defs.bzl", "java_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = ["Main.java"],
    deps = [":exports_my_simple_annotation_import"],
)

j2cl_library(
    name = "exports_my_simple_annotation_import",
    exports = [":my_simple_annotation_import"],
)

j2cl_import(
    name = "my_simple_annotation_import",
    jar = ":my_simple_annotation",
)

java_library(
    name = "my_simple_annotation",
    srcs = [
        "MySimpleAnnotation.java",
    ],
)
