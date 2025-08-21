load("//build_defs:rules.bzl", "j2kt_jvm_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

# Exclude J2ktMonitor.java from the readable example because it is part of the J2kt library.
j2kt_jvm_library(
    name = "readable-j2kt-jvm",
    srcs = glob(
        ["*.java"],
        exclude = ["J2ktMonitor.java"],
    ),
    j2kt_j2objc_interop_enabled = False,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2kt-jvm",
    ],
)

readable_example(
    srcs = glob(["*.java"]),
    # TODO(b/439710157): Re-enable once the bug is fixed.
    build_kt_native_readables = False,
    deps = ["//jre/java:javaemul_internal_annotations-j2cl"],
)
