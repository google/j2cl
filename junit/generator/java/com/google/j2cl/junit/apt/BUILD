load("//build_defs:rules.bzl", "j2cl_library")
load("@rules_java//java:defs.bzl", "java_library", "java_plugin")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "internal_junit_annotations",
    testonly = 1,
    srcs = ["J2clTestInput.java"],
    exported_plugins = [":junit_processor_plugin"],
    visibility = ["//build_defs/internal_do_not_use:__pkg__"],
)

java_plugin(
    name = "junit_processor_plugin",
    testonly = 1,
    generates_api = True,
    processor_class = "com.google.j2cl.junit.apt.J2clTestingProcessor",
    deps = [":junit_processor"],
)

java_library(
    name = "junit_processor",
    testonly = 1,
    srcs = glob(["*.java"]),
    resources = glob(["*.vm"]),
    visibility = ["//junit/generator/javatests/com/google/j2cl/junit/apt:__pkg__"],
    deps = [
        "//junit/generator/java/com/google/j2cl/junit/async",
        "//third_party:auto_common",
        "//third_party:auto_service",
        "//third_party:auto_value",
        "//third_party:escapevelocity",
        "//third_party:guava",
        "//third_party:jsinterop-annotations",
        "//third_party:jsr305_annotations",
        "//third_party:junit",
    ],
)
