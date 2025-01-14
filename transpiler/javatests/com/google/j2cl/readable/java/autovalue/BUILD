load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "j2kt_incompatible",
    srcs = ["J2ktIncompatible.java"],
)

# TODO(b/317166154): Port this test to Kotlin when AutoValue is supported.
readable_example(
    srcs = glob(
        ["*.java"],
        exclude = ["J2ktIncompatible.java"],
    ),
    generate_kt_web_readables = True,
    deps = [
        ":j2kt_incompatible",
        "//third_party:auto_value_generates_api-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
        "//third_party:jsr305_annotations-j2cl",
    ],
)
