load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = ["TypeAnnotations.java"],
    # TODO(b/202428351): Add support for generics.
    build_kt_readables = False,
    deps = [
        ":parametric-type",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)

j2cl_library(
    name = "parametric-type",
    # ParametricType is explicitly defined in a separate library to test propagation of
    # type annotations across compilation boundaries.
    srcs = ["ParametricType.java"],
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)
