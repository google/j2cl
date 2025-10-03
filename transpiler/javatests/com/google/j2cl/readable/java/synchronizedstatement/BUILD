load("//build_defs:rules.bzl", "j2cl_library", "j2kt_jvm_library", "j2kt_native_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

# J2ktMonitor is part of the J2KT JRE so we don't need to include it.
# As we cannot define a completely empty j2kt_jvm_library, we export the readable's other deps.
j2kt_jvm_library(
    name = "deps-j2kt-jvm",
    j2kt_j2objc_interop_enabled = False,
    exports = ["//jre/java:javaemul_internal_annotations-j2kt-jvm"],
)

j2kt_native_library(
    name = "deps-j2kt-native",
    j2kt_j2objc_interop_enabled = False,
    exports = ["//jre/java:javaemul_internal_annotations-j2kt-native"],
)

# All remaining platforms need a J2ktMonitor stub.
j2cl_library(
    name = "deps-j2cl",
    srcs = ["J2ktMonitor.java"],
    exports = ["//jre/java:javaemul_internal_annotations-j2cl"],
)

readable_example(
    srcs = glob(
        ["*.java"],
        exclude = ["J2ktMonitor.java"],
    ),
    # TODO(b/439710157): Re-enable once the bug is fixed.
    build_kt_native_readables = False,
    deps = [":deps-j2cl"],
)
