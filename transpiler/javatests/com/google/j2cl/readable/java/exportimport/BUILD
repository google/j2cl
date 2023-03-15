load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)
load("//build_defs:rules.bzl", "j2cl_import", "j2cl_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = ["Main.java"],
    deps = [":exports_my_simple_annoation_import"],
)

j2cl_library(
    name = "exports_my_simple_annoation_import",
    exports = [":my_simple_annoation_import"],
)

j2cl_import(
    name = "my_simple_annoation_import",
    jar = ":my_simple_annoation",
)

java_library(
    name = "my_simple_annoation",
    srcs = [
        "MySimpleAnnotation.java",
    ],
)
