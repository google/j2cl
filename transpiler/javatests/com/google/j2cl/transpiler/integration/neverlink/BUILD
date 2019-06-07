package(
    licenses = ["notice"],  # Apache 2.0
)

load("//build_defs:rules.bzl", "j2cl_import", "j2cl_library")

java_library(
    name = "target_with_never_link",
    srcs = ["NeverLinkAnnotation.java"],
    neverlink = 1,
)

j2cl_import(
    name = "target_with_never_link-j2cl",
    jar = ":target_with_never_link",
)

j2cl_library(
    name = "neverlink",
    srcs = ["UsesNeverLink.java"],
    deps = [":target_with_never_link-j2cl"],
)
